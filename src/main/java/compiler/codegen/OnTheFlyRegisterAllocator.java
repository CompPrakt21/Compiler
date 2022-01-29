package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.DumpSir;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class OnTheFlyRegisterAllocator {

    private final SirGraph graph;
    private final StackSlots stackSlots;
    private final RegisterManager freeRegisters;
    private final List<VirtualRegister> methodParameters;

    /**
     * The set of virtual registers that appear in multiple different basic blocks.
     */
    private final Set<VirtualRegister> interBlockRegisters;

    /**
     * Virtual registers that appear in multiple basic blocks have to always be
     * mapped to the same hardware register, as is required by linear scan register allocation.
     * This map stores this mapping. Its keys can only be the virtual registers as are
     * stored in interBlockRegisters.
     */
    private final Map<VirtualRegister, HardwareRegister> interBlockRegisterAssignment;

    /**
     * Which virtual registers are live in hardware registers at the entry of a basic block.
     * For every incoming edge this has to be the same.
     * The first time the allocator encounters the basic block it defines this based on the current
     * register mapping.
     * Every subsequent incoming edge ensures the same register mapping.
     */
    private final Map<BasicBlock, Set<VirtualRegister>> blockIns;

    /**
     * Stores whether a virtual register has a *new* result.
     * The virtual register gets initialized with a new value (for example the result of calculation)
     * we might need to save the value to the stack if the hardware register is needed elsewhere.
     * However, if its value is just the loaded value from the stack and doesn't change,
     * (Which is likely because we are lowering from SSA)
     * there is no need to save it again to the stack.
     */
    private final Map<VirtualRegister, Boolean> dirty;

    /**
     * The sub instruction which allocates stack space for local variables.
     * After register allocation is finished (and all required virtual registers are spilled)
     * we know how much stackspace we need and update this value.
     * The same is true for when we free the allocated stack space before ret instructions.
     */
    private Optional<SubInstruction> allocateStackSpaceInstruction;
    private final List<AddInstruction> freeStackSpaceInstructions;

    private GlobalRegisterLifetimes.Lifetimes lifetimes;

    private final String name;
    private final boolean dumpGraphs;

    /**
     * Hints in which hardware register(s) the virtual register will be needed.
     */
    private Map<VirtualRegister, List<HardwareRegister.Group>> registerHints;

    public OnTheFlyRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph, String name, boolean dumpGraphs) {
        this.methodParameters = methodParameters;
        this.graph = graph;
        this.allocateStackSpaceInstruction = Optional.empty();
        this.freeStackSpaceInstructions = new ArrayList<>();
        this.stackSlots = new StackSlots();
        this.freeRegisters = new RegisterManager();
        this.dirty = new HashMap<>();

        this.registerHints = null;
        this.lifetimes = null;
        this.interBlockRegisterAssignment = new HashMap<>();
        this.interBlockRegisters = new HashSet<>();
        this.blockIns = new HashMap<>();

        this.name = name;
        this.dumpGraphs = dumpGraphs;
    }

    /**
     * Every virtual register is replaced with a hardware register.
     * Mutates the graph and schedule if necessary.
     */
    public void allocate() {
        // Calculate parameter offsets.
        var paramOffset = 2 * Register.Width.BIT64.getByteSize(); // RIP and RBP are before parameters
        for (VirtualRegister param : this.methodParameters) {
            this.stackSlots.mapRegister(param, paramOffset);
            paramOffset += Register.Width.BIT64.getByteSize();
        }

        // Determine virtual register lifetimes and schedules blocks.
        this.lifetimes = GlobalRegisterLifetimes.calculateLifetimes(this.graph);

        if (dumpGraphs) {
            try {
                new DumpSir(new PrintWriter(new File(String.format("sir-block-sched_%s.dot", this.name))), this.graph).withBlockSchedule(true).withInstructionIndices(true).dump();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Collect intra block registers
        var virtualRegistersInOtherBlocks = new HashSet<VirtualRegister>();
        for (var bb : this.graph.getBlocks()) {
            var registersInThisBlock = new HashSet<VirtualRegister>();
            for (var instr : bb.getInstructions()) {
                var usedRegs = Stream.concat(instr.getReadRegisters().stream(), instr.getWrittenRegister().stream())
                        .map(reg -> (VirtualRegister) reg)
                        .collect(Collectors.toCollection(HashSet::new));

                for (var usedReg : usedRegs) {
                    if (virtualRegistersInOtherBlocks.contains(usedReg)) {
                        this.interBlockRegisters.add(usedReg);
                    }
                }
                registersInThisBlock.addAll(usedRegs);
            }

            virtualRegistersInOtherBlocks.addAll(registersInThisBlock);
        }

        // Collec register hints.
        this.registerHints = collectRegisterHints(this.graph);

        // Initialize blockIns for start block
        this.blockIns.put(this.graph.getStartBlock(), Set.of());

        // Define interblock assignment for arguments
        for (var virtArgReg : this.methodParameters) {
            var hint = this.registerHints.get(virtArgReg);
            HardwareRegister.Group target;
            if (hint != null) {
                target = hint.get(0);
            } else {
                target = new ArrayList<>(this.freeRegisters.availableRegisters()).get(0);
            }
            this.interBlockRegisterAssignment.put(virtArgReg, target.getRegister(virtArgReg.getWidth()));
        }

        // Actually do the register allocation
        for (int i = 0; i < this.graph.getBlocks().size(); i++) {
            var bb = this.graph.getBlocks().get(i);
            var startInstructionIndex = this.graph.getStartInstructionIndices().get(i);
            this.allocateBasicBlock(bb, startInstructionIndex);
        }

        var stackOffset = this.stackSlots.getNeededStackSpace();

        var allocateStackSpace = this.allocateStackSpaceInstruction.orElseThrow();
        var subRhs = (Constant) allocateStackSpace.getRhs();
        subRhs.setValue(-stackOffset);

        this.freeStackSpaceInstructions.forEach(addInstr -> {
            var addRhs = (Constant) addInstr.getRhs();
            addRhs.setValue(-stackOffset);
        });
    }

    private HardwareRegister selectAndFreeTarget(
            VirtualRegister register,
            Optional<HardwareRegister.Group> target,
            List<HardwareRegister.Group> preferred,
            List<HardwareRegister.Group> disallowed,
            Set<HardwareRegister.Group> keepAlive,
            List<Instruction> newList
    ) {
        assert target.isEmpty() || !disallowed.contains(target.get());
        var mapping = this.freeRegisters.getMapping(register);

        Optional<HardwareRegister.Group> chosenTarget = Optional.empty();
        if (target.isPresent()) {
            chosenTarget = target;

            // Make sure target is free (if the virtual register is already mapped to target, there is no need.)
            if (!this.freeRegisters.isAvailable(target.get()) && !(mapping.isPresent() && target.get().equals(mapping.get().getGroup()))) {
                assert !keepAlive.contains(target.get());
                this.makeUnusedSpecificRegister(target.get().getRegister(register.getWidth()), newList);
            }
        } else if (mapping.isPresent() && !disallowed.contains(mapping.get().getGroup())) {
            // If it is already mapped we take it.

            chosenTarget = mapping.map(HardwareRegister::getGroup);
        } else {
            // There is no obvious choice, so we search in the free registers for a suitable candidate.
            for (var availableRegister : this.freeRegisters.availableRegisters()) {
                if (disallowed.contains(availableRegister)) {
                    continue;
                }

                if (preferred.contains(availableRegister)) {
                    chosenTarget = Optional.of(availableRegister);
                    break;
                }

                if (chosenTarget.isEmpty()) {
                    chosenTarget = Optional.of(availableRegister);

                    if (preferred.isEmpty()) break;
                }
            }

            // No free register can be used, so lets choose any of the preferred or, if even that fails, any random register.
            if (chosenTarget.isEmpty()) {
                chosenTarget = preferred.stream()
                        .filter(reg -> !disallowed.contains(reg) && !keepAlive.contains(reg))
                        .findFirst()
                        .or(() -> this.freeRegisters.getMapping()
                                .rightSet()
                                .stream()
                                .filter(reg -> !disallowed.contains(reg) && !keepAlive.contains(reg))
                                .findFirst()
                        );

                this.makeUnusedSpecificRegister(chosenTarget.orElseThrow().getRegister(register.getWidth()), newList);
            }
        }

        var targetRegister = chosenTarget.orElseThrow().getRegister(register.getWidth());

        // Register is mapped, but the currently mapped hardware register is disallowed.
        // Therefore we need to move the value into a new (allowed) hardware register.
        // Ideally the mapping should be created by the calling functions, but they don't have the information
        // that the value is actually in a different register.
        // This might break initializeVirtualRegister...
        if (mapping.isPresent() && disallowed.contains(mapping.get().getGroup())) {
            var width = register.getWidth();

            newList.add(new MovInstruction(width, targetRegister, mapping.get()));
            this.freeRegisters.freeMapping(register);
            this.freeRegisters.createSpecificMapping(register, targetRegister);
        }

        return targetRegister;
    }

    /**
     * Map virtual register to hardware register so it can be used as the result register
     * of some instruction.
     *
     * @param register The virtual register to be used.
     * @param target The hardware register it should be mapped to. If this is not empty, the return value will be its content.
     * @param preferred If possible one of these hardware registers should be chosen.
     * @param disallowed Under no circumstances should the target hardware register be one of these.
     * @param keepAlive Registers must be unchanged. They might be used in other parts of the current instruction.
     *                  It is assumed that they are not free.
     */
    private HardwareRegister initialiseVirtualRegister(
            VirtualRegister register,
            Optional<HardwareRegister.Group> target,
            List<HardwareRegister.Group> preferred,
            List<HardwareRegister.Group> disallowed,
            Set<HardwareRegister.Group> keepAlive,
            List<Instruction> newList
    ) {

        var targetRegister = this.selectAndFreeTarget(register, target, preferred, disallowed, keepAlive, newList);

        // Are we initialising an inter block register for the first time?
        if (!this.interBlockRegisterAssignment.containsKey(register) && this.interBlockRegisters.contains(register)) {
            // We remember this and on any backedge we need to make sure the virtual register is mapped to *this* target register.
            this.interBlockRegisterAssignment.put(register, targetRegister);
        }

        // Due to phi nodes, some virtual register might be initialized multple times.
        if (this.freeRegisters.getMapping(register).isPresent()) {
            this.freeRegisters.freeMapping(register);
        }
        // Only registers which get written to *new* values are initialised.
        this.dirty.put(register, true);
        this.freeRegisters.createSpecificMapping(register, targetRegister).orElseThrow();

        return targetRegister;
    }

    /**
     * Load the value that the virtual register is mapped to, into a hardware register.
     * If virtual register is written, use initializeVirtualRegister.
     *
     * @param register The virtual register to be used.
     * @param target The hardware register it should be mapped to. If this is not empty, the return value will be its content.
     * @param preferred If possible one of these hardware registers should be chosen.
     * @param disallowed Under no circumstances should the target hardware register be one of these.
     * @param keepAlive Registers must be unchanged. They might be used in other parts of the current instruction.
     *                  It is assumed that they are not free.
     */
    private HardwareRegister concretizeVirtualRegister(
            VirtualRegister register,
            Optional<HardwareRegister.Group> target,
            List<HardwareRegister.Group> preferred,
            List<HardwareRegister.Group> disallowed,
            Set<HardwareRegister.Group> keepAlive,
            List<Instruction> newList
    ) {

        var targetRegister = this.selectAndFreeTarget(register, target, preferred, disallowed, keepAlive, newList);
        var mapping = this.freeRegisters.getMapping(register);

        // Now that we have the target and made sure it is free to be used we need to move the actual value into it.

        if (mapping.isPresent() && mapping.get().equals(targetRegister)) {
            // Already mapped correctly no need to do anything.
            return targetRegister;
        } else if (mapping.isPresent()) {
            // Mapped, but to wrong register.
            newList.add(new MovInstruction(register.getWidth(), targetRegister, mapping.get()));
            this.freeRegisters.freeMapping(register);
            this.freeRegisters.createSpecificMapping(register, targetRegister);
        } else {
            // It needs to be loaded from memory.
            var offset = this.stackSlots.get(register);
            newList.add(new MovInstruction(register.getWidth(), targetRegister, new MemoryLocation(HardwareRegister.RBP, offset)));
            // This value is loaded from the stack.
            // If it needs to be free'd, don't write it back again to the stack.
            this.dirty.put(register, false);
            this.freeRegisters.createSpecificMapping(register, targetRegister).orElseThrow();
        }

        return targetRegister;
    }

    private void saveVirtualRegister(VirtualRegister virtReg, HardwareRegister value, List<Instruction> newList)  {
        assert this.dirty.containsKey(virtReg);
        if (this.dirty.get(virtReg)) {
            var offset = this.stackSlots.get(virtReg);
            newList.add(new MovInstruction(virtReg.getWidth(), new MemoryLocation(HardwareRegister.RBP, offset), value));
        }
        this.dirty.remove(virtReg);
    }

    private void makeUnusedSpecificRegister(HardwareRegister register, List<Instruction> newList) {
        if (!this.freeRegisters.isAvailable(register.getGroup())) {
            var associatedVirtRegister = this.freeRegisters.getMappedVirtualRegister(register.getGroup());
            this.saveVirtualRegister(associatedVirtRegister, register.getGroup().getRegister(associatedVirtRegister.getWidth()), newList);
            this.freeRegisters.freeMapping(associatedVirtRegister);
        }
    }

    private void freeAllRegisters(List<Instruction> newList) {
        List<VirtualRegister> regsToBeFreed = new ArrayList<>();
        for (var virtReg : this.freeRegisters.getMapping().leftSet()) {
            var hardwareReg = this.freeRegisters.getMapping(virtReg).orElseThrow();

            this.saveVirtualRegister(virtReg, hardwareReg, newList);

            regsToBeFreed.add(virtReg);
        }

        regsToBeFreed.forEach(this.freeRegisters::freeMapping);
    }

    private HardwareRegister initialiseVirtualRegisterInto(VirtualRegister virtReg, HardwareRegister target, List<Instruction> newList) {
        return this.initialiseVirtualRegister(virtReg, Optional.of(target.getGroup()), List.of(), List.of(), Set.of(), newList);
    }

    private HardwareRegister initialiseVirtualRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        var pref = Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of);
        return this.initialiseVirtualRegister(virtReg, Optional.empty(), pref, List.of(), a, newList);
    }

    private HardwareRegister initialiseVirtualRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive, List<HardwareRegister> dontChoose, Set<HardwareRegister> hint) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        List<HardwareRegister.Group> pref = Stream.concat(Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of).stream(), hint.stream().map(HardwareRegister::getGroup)).toList();
        return this.initialiseVirtualRegister(virtReg, Optional.empty(), pref, dontChoose.stream().map(HardwareRegister::getGroup).toList(), a, newList);
    }

    private HardwareRegister concretizeRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        var pref = Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of);
        return this.concretizeVirtualRegister(virtReg, Optional.empty(), pref, List.of(), a, newList);
    }

    private HardwareRegister concretizeRegisterWithout(VirtualRegister virtReg, List<Instruction> newList, List<HardwareRegister> disallowed, Set<HardwareRegister> keepAlive) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        List<HardwareRegister.Group> d = disallowed.stream().map(HardwareRegister::getGroup).toList();
        return this.concretizeVirtualRegister(virtReg, Optional.empty(), List.of(), d, a, newList);
    }

    private HardwareRegister concretizeRegisterInto(HardwareRegister target, VirtualRegister virtReg, List<Instruction> newList) {
        var pref = Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of);
        return this.concretizeVirtualRegister(virtReg, Optional.of(target.getGroup()), pref, List.of(), Set.of(), newList);
    }

    /**
     * replaces virtual registers in a memory loation with free hardware registers and adds the necessary load
     * instructions.
     *
     * @param except If register pressure needs to be reduced, no mapping with virtual registers in except will be freed.
     */
    private List<HardwareRegister> concretizeMemoryLocation(MemoryLocation loc, List<Instruction> newList, Set<HardwareRegister> except) {
        List<HardwareRegister> mappings = new ArrayList<>();

        if (loc.getBaseRegister().isPresent() && loc.getBaseRegister().get() instanceof VirtualRegister virtReg) {
            var hardwareBaseReg = this.concretizeRegister(virtReg, newList, except);
            loc.setBaseRegister(hardwareBaseReg);
            mappings.add(hardwareBaseReg);
        }

        if (loc.getIndex().isPresent() && loc.getIndex().get() instanceof VirtualRegister virtReg) {
            var updatedExcept = Stream.concat(except.stream(), mappings.stream()).collect(Collectors.toSet());
            var hardwareIndexReg = this.concretizeRegister(virtReg, newList, updatedExcept);
            loc.setIndex(hardwareIndexReg);
            mappings.add(hardwareIndexReg);
        }

        return mappings;
    }

    /**
     * Frees mapped virtual registers that have died.
     * This method should be called after all read registers have been concretized,
     * but before the written registers are initialized, to allow read registers that are dead to
     * be reused immediately.
     */
    private void freeDeadVirtualRegisters(Set<VirtualRegister> liveRegs) {
        var deadRegs = this.freeRegisters.getMapping().leftSet().stream().filter(virtReg -> !liveRegs.contains(virtReg)).toList();
        deadRegs.forEach(this.dirty::remove);
        deadRegs.forEach(this.freeRegisters::freeMapping);
    }

    /**
     * Register the blockIns of a basic block.
     * If the provided blockIns conflict with their target in interBlockRegisterAssignment,
     * the set is reduced until all conflicts are resolved.
     * @return the possibly reduced set of blockIns
     */
    private Set<VirtualRegister> registerBlockIns(BasicBlock bb, Set<VirtualRegister> blockIns) {
        var counted = new HashMap<HardwareRegister.Group, Integer>();
        for (var virtReg : blockIns) {
            var group = this.interBlockRegisterAssignment.get(virtReg).getGroup();
            counted.putIfAbsent(group, 0);
            counted.put(group, counted.get(group) + 1);
        }

        var newBlockIns = new HashSet<VirtualRegister>();
        for (var virtReg : blockIns) {
            var group = this.interBlockRegisterAssignment.get(virtReg).getGroup();
            if (counted.get(group) == 1) {
                newBlockIns.add(virtReg);
            } else {
                counted.put(group, counted.get(group) - 1);
            }
        }

        assert !this.blockIns.containsKey(bb);
        this.blockIns.put(bb, newBlockIns);

        return newBlockIns;
    }

    /**
     * Saves and concretizes registers required by the next basic block(s)
     * @param targetBlockIns The blockIns needed.
     */
    private void prepareRegisterMappingForBlockIns(Set<VirtualRegister> targetBlockIns, List<Instruction> newList) {
        // Save registers that do not appear in the targets blockIns.
        var virtRegsNotInBlockIn = new ArrayList<VirtualRegister>();
        for (var pair : this.freeRegisters.getMapping().entrySet()) {
            var liveReg = pair.getKey();
            if (!targetBlockIns.contains(liveReg)) {
                virtRegsNotInBlockIn.add(liveReg);
            }
        }

        for (var liveReg : virtRegsNotInBlockIn) {
            var targetReg = this.freeRegisters.getMapping(liveReg).orElseThrow();
            this.saveVirtualRegister(liveReg, targetReg, newList);
            this.freeRegisters.freeMapping(liveReg);
        }

        // Concretize registers that appear in the targets blockIns.
        for (var expectedLiveReg : targetBlockIns) {
            var expectedTargetReg = this.interBlockRegisterAssignment.get(expectedLiveReg);
            var currentMapping = this.freeRegisters.getMapping(expectedLiveReg);

            if (currentMapping.isEmpty()) {
                this.concretizeRegisterInto(expectedTargetReg, expectedLiveReg, newList);
            } else if (!currentMapping.get().equals(expectedTargetReg)) {
                this.makeUnusedSpecificRegister(expectedTargetReg, newList);
                newList.add(new MovInstruction(expectedLiveReg.getWidth(), expectedTargetReg, currentMapping.get()));
                this.freeRegisters.freeMapping(expectedLiveReg);
                this.freeRegisters.createSpecificMapping(expectedLiveReg, expectedTargetReg);
            }
        }
    }

    private void allocateRegForInstruction(Instruction instr, Set<VirtualRegister> liveRegs, List<Instruction> newList) {
        switch (instr) {
            case DivInstruction div -> {
                var dividendVirtReg = (VirtualRegister) div.getDividend();
                var divisorVirtReg = (VirtualRegister) div.getDivisor();

                var divisorHardwareReg = this.concretizeRegisterWithout(divisorVirtReg, newList, List.of(HardwareRegister.EDX, HardwareRegister.EAX), Set.of());

                HardwareRegister dividendHardwareReg;
                if (!liveRegs.contains(dividendVirtReg)) {
                    // We can load dividend directly into EAX because it is dead after this instruction.
                    dividendHardwareReg = this.concretizeRegisterInto(HardwareRegister.EAX, dividendVirtReg, newList);
                } else {
                    dividendHardwareReg = this.concretizeRegisterWithout(dividendVirtReg, newList, List.of(HardwareRegister.EAX, HardwareRegister.EDX), Set.of(divisorHardwareReg));
                    this.makeUnusedSpecificRegister(HardwareRegister.EAX, newList);
                    newList.add(new MovInstruction(dividendHardwareReg.getWidth(), HardwareRegister.EAX, dividendHardwareReg));
                }

                this.makeUnusedSpecificRegister(HardwareRegister.EDX, newList);

                this.freeDeadVirtualRegisters(liveRegs);

                newList.add(new ConvertDoubleToQuadInstruction(HardwareRegister.EDX, HardwareRegister.EAX));
                newList.add(div);

                var targetVirtReg = (VirtualRegister) div.getTarget();
                var targetHardwareReg = switch (div.getType()) {
                    case Div -> HardwareRegister.EAX;
                    case Mod -> HardwareRegister.EDX;
                };

                this.dirty.put(targetVirtReg, true);
                this.freeRegisters.createSpecificMapping(targetVirtReg, targetHardwareReg);

                div.setTarget(targetHardwareReg);
                div.setDividend(HardwareRegister.EAX);
                div.setDivisor(divisorHardwareReg);
            }
            case BinaryInstruction binary -> {
                var usedRegisters = new HashSet<HardwareRegister>();

                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsVirtReg = (VirtualRegister) binary.getLhs();

                var lhsReg= this.concretizeRegister(lhsVirtReg, newList, Set.of());
                usedRegisters.add(lhsReg);

                List<HardwareRegister> mustStayLive = List.of();
                if (binary.getRhs() instanceof MemoryLocation rhs) {
                    var memLocRegs = this.concretizeMemoryLocation(rhs, newList, usedRegisters);
                    usedRegisters.addAll(memLocRegs);
                    mustStayLive = memLocRegs;
                } else if (binary.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList, usedRegisters);
                    mustStayLive = List.of(rhsHardwareReg);
                    usedRegisters.add(rhsHardwareReg);
                    binary.setRhs(rhsHardwareReg);
                }

                // If there is a rhs register different from the lhs register, it is important that the target hardware register
                // doesn't allocate the same register because it would get overwritten by the mov inserted before the binary instruction.
                // However, if they are the same virtual register this is ok.
                if (!mustStayLive.isEmpty()) {
                    mustStayLive = mustStayLive.stream().filter(r -> !r.equals(lhsReg)).toList();
                }

                this.freeDeadVirtualRegisters(liveRegs);

                var targetHardwareReg = this.initialiseVirtualRegister(targetReg, newList, usedRegisters, mustStayLive, Set.of(lhsReg));
                if (targetHardwareReg != lhsReg) {
                    newList.add(new MovInstruction(targetHardwareReg.getWidth(), targetHardwareReg, lhsReg));
                }

                binary.setTarget(targetHardwareReg);
                binary.setLhs(targetHardwareReg);

                newList.add(binary);
            }
            case ReturnInstruction ret -> {
                // Load return value into RAX/EAX
                if (ret.getReturnValue().isPresent()) {
                    var retVirtReg = (VirtualRegister)ret.getReturnValue().get();
                    var hardwareReg = this.concretizeRegisterInto(HardwareRegister.EAX.forWidth(retVirtReg.getWidth()), retVirtReg, newList);
                    ret.setReturnValue(hardwareReg);
                }

                this.freeDeadVirtualRegisters(liveRegs);

                // Function epilog
                var freeStackSpace = new AddInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(0));
                this.freeStackSpaceInstructions.add(freeStackSpace);

                newList.add(freeStackSpace);
                newList.add(new LeaveInstruction());
                newList.add(ret);
            }
            case JumpInstruction jump -> {
                this.freeDeadVirtualRegisters(liveRegs);

                // Prepare the live registers to fit the target block ins.
                var targetBlockIns = this.blockIns.get(jump.getTarget());
                if (targetBlockIns == null) {
                    // Set the blockIns of the target block if they are undefined.
                    targetBlockIns = new HashSet<>(this.freeRegisters.getMapping().leftSet());
                    targetBlockIns = this.registerBlockIns(jump.getTarget(), targetBlockIns);
                }

                this.prepareRegisterMappingForBlockIns(targetBlockIns, newList);

                newList.add(jump);
            }
            case CmpInstruction cmp -> {
                var lhsVirtReg = (VirtualRegister) cmp.getLhs();
                var lhsHardwareReg = this.concretizeRegister(lhsVirtReg, newList, Set.of());

                if (cmp.getRhs() instanceof MemoryLocation rhs) {
                    this.concretizeMemoryLocation(rhs, newList, Set.of(lhsHardwareReg));
                } else if (cmp.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList, Set.of(lhsHardwareReg));
                    cmp.setRhs(rhsHardwareReg);
                }

                this.freeDeadVirtualRegisters(liveRegs);

                cmp.setLhs(lhsHardwareReg);
                newList.add(cmp);
            }
            case BranchInstruction branch -> {
                this.freeDeadVirtualRegisters(liveRegs);

                Set<VirtualRegister> accumulatedBlockIns = new HashSet<>();

                var trueBlockIns = this.blockIns.get(branch.getTrueBlock());
                var falseBlockIns = this.blockIns.get(branch.getFalseBlock());

                if (trueBlockIns != null && falseBlockIns != null) {
                    // Check for target hardware register collisions
                    var intersectionBlockIns = new HashSet<>(trueBlockIns);
                    intersectionBlockIns.retainAll(falseBlockIns);
                    var xorBlockIns = new HashSet<>(trueBlockIns);
                    xorBlockIns.addAll(falseBlockIns);
                    xorBlockIns.removeAll(intersectionBlockIns);

                    var xorTargetRegs = xorBlockIns.stream().map(this.interBlockRegisterAssignment::get).collect(Collectors.toSet());
                    if (xorBlockIns.size() != xorTargetRegs.size()) {
                        // collision detected
                        assert false : "The program contains a weird edge case I hoped would not occur, please report this bug";
                    } else {
                        // no collision
                        accumulatedBlockIns.addAll(trueBlockIns);
                        accumulatedBlockIns.addAll(falseBlockIns);
                    }
                } else if (trueBlockIns == null && falseBlockIns != null) {
                    accumulatedBlockIns = this.registerBlockIns(branch.getTrueBlock(), falseBlockIns);
                } else if (trueBlockIns != null) {
                    accumulatedBlockIns = this.registerBlockIns(branch.getFalseBlock(), trueBlockIns);
                } else {
                    var blockIns = new HashSet<>(this.freeRegisters.getMapping().leftSet());
                    accumulatedBlockIns = this.registerBlockIns(branch.getTrueBlock(), blockIns);
                    if (!branch.getFalseBlock().equals(branch.getTrueBlock())) {
                        this.registerBlockIns(branch.getFalseBlock(), blockIns);
                    }
                }

                this.prepareRegisterMappingForBlockIns(accumulatedBlockIns, newList);

                newList.add(branch);
            }
            case AllocCallInstruction allocCall -> {
                var objectSizeVirtReg = (VirtualRegister) allocCall.getObjectSize();
                var objectSizeReg = this.concretizeRegisterInto(HardwareRegister.EDI, objectSizeVirtReg, newList);
                var numElementsVirtReg = (VirtualRegister) allocCall.getNumElements();
                var numElementsReg = this.concretizeRegisterInto(HardwareRegister.ESI, numElementsVirtReg, newList);

                // First free registers that have died, then save all others.
                this.freeDeadVirtualRegisters(liveRegs);
                this.freeAllRegisters(newList);

                var virtRegTarget = (VirtualRegister) allocCall.getTarget();

                allocCall.setNumElements(numElementsReg);
                allocCall.setObjectSize(objectSizeReg);

                newList.add(allocCall);

                this.freeRegisters.clearAllMappings();

                this.initialiseVirtualRegisterInto(virtRegTarget, HardwareRegister.RAX, newList);

                allocCall.setTarget(HardwareRegister.RAX);
            }
            case MethodCallInstruction methodCall -> {
                switch (methodCall.getMethod()) {
                    case IntrinsicMethod intrinsic -> {
                        assert methodCall.getArguments().size() <= 1;

                        // Intrinsic has an argument, so we load it into edi
                        if (methodCall.getArguments().size() == 1) {
                            var argVirtReg = (VirtualRegister) methodCall.getArguments().get(0);
                            var argReg= this.concretizeRegisterInto(HardwareRegister.EDI, argVirtReg, newList);
                            methodCall.getArguments().set(0, argReg);
                        }

                        this.freeDeadVirtualRegisters(liveRegs);
                        this.freeAllRegisters(newList);

                        newList.add(methodCall);
                        this.freeRegisters.clearAllMappings();

                        if (!(intrinsic.getReturnTy() instanceof VoidTy)) {
                            var targetVirtReg = (VirtualRegister)methodCall.getTarget();
                            // The only intrinsic function that has a return value is System.in.read which returns a 32bit int.
                            var targetHardwareReg = this.initialiseVirtualRegisterInto(targetVirtReg, HardwareRegister.EAX, newList);
                            methodCall.setTarget(targetHardwareReg);
                        }
                    }
                    case DefinedMethod method -> {
                        for (int i = methodCall.getArguments().size() - 1; i >= 0; i--) {
                            var virtReg = (VirtualRegister) methodCall.getArguments().get(i);
                            var hardwareReg = this.concretizeRegister(virtReg, newList, Set.of());

                            newList.add(new PushInstruction(hardwareReg.forWidth(Register.Width.BIT64)));
                        }
                        var requiredStackSpace = methodCall.getArguments().size() * Register.Width.BIT64.getByteSize();

                        this.freeDeadVirtualRegisters(liveRegs);
                        this.freeAllRegisters(newList);

                        // call function
                        newList.add(methodCall);

                        // save return value;
                        if (!(method.getReturnTy() instanceof VoidTy)) {
                            var targetVirtReg = (VirtualRegister) methodCall.getTarget();

                            // The only intrinsic function that has a return value is System.in.read which returns a 32bit int.
                            var targetHardwareReg = this.initialiseVirtualRegisterInto(targetVirtReg, HardwareRegister.Group.A.getRegister(targetVirtReg.getWidth()), newList);
                            methodCall.setTarget(targetHardwareReg);
                        }

                        // free stack space
                        newList.add(new AddInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(requiredStackSpace)));
                    }
                }
            }
            case MovInstruction mov -> {
                var usedRegisters = new HashSet<HardwareRegister>();

                if (mov.getSource() instanceof VirtualRegister source) {
                    var sourceHardwareReg = this.concretizeRegister(source, newList, usedRegisters);
                    usedRegisters.add(sourceHardwareReg);
                    mov.setSource(sourceHardwareReg);
                } else if (mov.getSource() instanceof MemoryLocation source) {
                    usedRegisters.addAll(this.concretizeMemoryLocation(source, newList, usedRegisters));
                }

                switch (mov.getDestination()) {
                    case MemoryLocation loc -> {
                        usedRegisters.addAll(this.concretizeMemoryLocation(loc, newList, usedRegisters));
                        this.freeDeadVirtualRegisters(liveRegs);
                        newList.add(mov);
                    }
                    case VirtualRegister virtualReg -> {
                        this.freeDeadVirtualRegisters(liveRegs);

                        // If virtualReg accumulates the phi values, it might be already mapped and is updated.
                        // In this case we need to set the dirty bit, so it gets saved at the end of the basic block.
                        var mapping = this.freeRegisters.getMapping(virtualReg);

                        // We hint that we want to use the same register as the source, to increase chance that this move will have the same source and
                        // destination, so that the peephole optimizer can remove this move.
                        var targetHardwareReg = mapping.orElseGet(() -> this.initialiseVirtualRegister(virtualReg, newList, usedRegisters, List.of(), usedRegisters));
                        mapping.ifPresent(ignored -> this.dirty.put(virtualReg, true));

                        usedRegisters.add(targetHardwareReg);
                        mov.setDestination(targetHardwareReg);
                        newList.add(mov);
                    }
                    default -> throw new AssertionError("Unexpected mov destination");
                }
            }
            case MovSignExtendInstruction movSX -> {
                var usedRegisters = new HashSet<HardwareRegister>();

                if (movSX.getInput() instanceof VirtualRegister virtRegInput) {
                    var inputHardwareReg = this.concretizeRegister(virtRegInput, newList, usedRegisters);
                    usedRegisters.add(inputHardwareReg);
                    movSX.setInput(inputHardwareReg);
                }

                this.freeDeadVirtualRegisters(liveRegs);

                if (movSX.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.initialiseVirtualRegister(virRegTarget, newList, usedRegisters);
                    usedRegisters.add(targetHardware);
                    movSX.setTarget(targetHardware);
                }

                newList.add(movSX);
            }
            case LoadEffectiveAddressInstruction lea -> {
                var usedRegisters = new HashSet<>(this.concretizeMemoryLocation(lea.getLoc(), newList, Set.of()));

                this.freeDeadVirtualRegisters(liveRegs);

                if (lea.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.initialiseVirtualRegister(virRegTarget, newList, usedRegisters);
                    usedRegisters.add(targetHardware);
                    lea.setTarget(targetHardware);
                }
                newList.add(lea);
            }
            default -> throw new AssertionError("These instructions should not appear pre register allocation");
        }
    }

    private void allocateBasicBlock(BasicBlock bb, int startInstructionIndex) {
        List<Instruction> newList = new ArrayList<>();

        var expectedLiveRegs = this.blockIns.get(bb);
        //// The block ins need to be defined before we can start allocate the basic block.
        //assert expectedLiveRegs != null;
        //// the expected live registers have to be a subset of the currently live registers.
        //assert this.freeRegisters.getMapping().leftSet().containsAll(expectedLiveRegs);
        //// The expected live registers are mapped to their expected respective hardware registers.
        //assert this.freeRegisters.getMapping().leftSet().stream().allMatch(reg -> this.freeRegisters.getMapping(reg).orElseThrow().equals(this.interBlockRegisterAssignment.get(reg)));
        //// BlockIns can only ever be registers that appear in multiple blocks. Otherwise these registers should have died by the end of their block.
        //assert this.interBlockRegisters.containsAll(expectedLiveRegs);

        // Remove live registers that are not expected.
        // These can occur when we are coming from a branch which needs to satisfy two sets of expected live registers. (each target block respectively)
        this.freeRegisters.clearAllMappings();
        expectedLiveRegs.forEach(reg -> {
            this.freeRegisters.createSpecificMapping(reg, this.interBlockRegisterAssignment.get(reg));
            // The register might be dirty, so we conservatively just set it to true and accept possibly unecessary stores.
            this.dirty.put(reg, true);
        });

        // add function prolog to the starting block
        if (bb == this.graph.getStartBlock()) {
            newList.add(new PushInstruction(HardwareRegister.RBP));
            newList.add(new MovInstruction(Register.Width.BIT64, HardwareRegister.RBP, HardwareRegister.RSP));
            var stackSpace = new SubInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(0));
            this.allocateStackSpaceInstruction = Optional.of(stackSpace);
            newList.add(stackSpace);
        }

        for (int i = 0; i < bb.getInstructions().size(); i++) {
            var instruction = bb.getInstructions().get(i);
            var liveRegisters = this.lifetimes.getLiveRegisters(startInstructionIndex + i);
            this.allocateRegForInstruction(instruction, liveRegisters, newList);
        }

        bb.setInstructions(newList);
    }

    private static Map<VirtualRegister, List<HardwareRegister.Group>> collectRegisterHints(SirGraph graph) {
        Map<VirtualRegister, List<HardwareRegister.Group>> result = new HashMap<>();

        BiConsumer<VirtualRegister, HardwareRegister.Group> add = (VirtualRegister reg, HardwareRegister.Group hint) -> {
            result.putIfAbsent(reg, new ArrayList<>());
            var l = result.get(reg);
            if (!l.contains(hint)) {
                l.add(hint);
            }
        };

        for (var bb : graph.getBlocks()) {
            for (var instr : bb.getInstructions()) {
                switch (instr) {
                    case ReturnInstruction ret -> {
                        if (ret.getReturnValue().isPresent()) {
                            add.accept((VirtualRegister) ret.getReturnValue().get(), HardwareRegister.Group.A);
                        }
                    }
                    case AllocCallInstruction alloc -> {
                        add.accept((VirtualRegister) alloc.getObjectSize(), HardwareRegister.Group.DI);
                        add.accept((VirtualRegister) alloc.getNumElements(), HardwareRegister.Group.SI);
                    }
                    case MethodCallInstruction call -> {
                        if (call.getMethod() instanceof IntrinsicMethod) {
                            if (!call.getArguments().isEmpty()) {
                                add.accept((VirtualRegister) call.getArguments().get(0), HardwareRegister.Group.DI);
                            }
                        }
                    }
                    case DivInstruction div -> add.accept((VirtualRegister) div.getDividend(), HardwareRegister.Group.D);
                    default -> {}
                }
            }
        }

        return result;
    }
}