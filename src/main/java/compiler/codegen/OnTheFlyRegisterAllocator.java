package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

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
     * The sub instruction which allocates stack space for local variables.
     * After register allocation is finished (and all required virtual registers are spilled)
     * we know how much stackspace we need and update this value.
     * The same is true for when we free the allocated stack space before ret instructions.
     */
    private Optional<SubInstruction> allocateStackSpaceInstruction;
    private final List<AddInstruction> freeStackSpaceInstructions;

    private RegisterLifetimes lifetimes;

    /**
     * Hints in which hardware register(s) the virtual register will be needed.
     */
    private Map<VirtualRegister, List<HardwareRegister.Group>> registerHints;

    public OnTheFlyRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph) {
        this.methodParameters = methodParameters;
        this.graph = graph;
        this.allocateStackSpaceInstruction = Optional.empty();
        this.freeStackSpaceInstructions = new ArrayList<>();
        this.stackSlots = new StackSlots();
        this.freeRegisters = new RegisterManager();

        this.registerHints = null;
        this.lifetimes = null;
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

        this.lifetimes = RegisterLifetimes.calculateLifetime(this.graph);
        this.registerHints = collectRegisterHints(this.graph);

        for (var bb : this.graph.getBlocks()) {
            this.allocateBasicBlock(bb);
        }

        var stackOffset = this.stackSlots.getNeededStackSpace();

        var allocateStackSpace = this.allocateStackSpaceInstruction.orElseThrow();
        var subRhs = (Constant) allocateStackSpace.getRhs();
        subRhs.setValue(-stackOffset);

        this.freeStackSpaceInstructions.forEach(addInstr -> {
            var addRhs = (Constant) addInstr.getRhs();
            addRhs.setValue(-stackOffset);
        });

        this.scheduleBlocks();
    }

    private HardwareRegister selectAndFreeTarget(
            VirtualRegister register,
            Optional<HardwareRegister.Group> target,
            List<HardwareRegister.Group> preferred,
            List<HardwareRegister.Group> disallowed,
            Set<HardwareRegister.Group> keepAlive,
            List<Instruction> newList
    ) {
        var mapping = this.freeRegisters.getMapping(register);

        Optional<HardwareRegister.Group> chosenTarget = Optional.empty();
        if (target.isPresent()) {
            chosenTarget = target;

            // Make sure target is free (if the virtual register is already mapped to target, there is no need.)
            if (!this.freeRegisters.isAvailable(target.get()) && !(mapping.isPresent() && target.get().equals(mapping.get().getGroup()))) {
                assert !keepAlive.contains(target.get());
                this.makeUnusedSpecificRegister(target.get().getRegister(register.getWidth()), newList);
            }
        } else if (mapping.isPresent()) {
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

        return chosenTarget.orElseThrow().getRegister(register.getWidth());
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

        // Due to phi nodes, some virtual register might be initialized multplie times.
        if (this.freeRegisters.getMapping(register).isPresent()) {
            this.freeRegisters.freeMapping(register);
        }
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
            this.freeRegisters.createSpecificMapping(register, targetRegister).orElseThrow();
        }

        return targetRegister;
    }

    private void saveVirtualRegister(VirtualRegister virtReg, HardwareRegister value, List<Instruction> newList)  {
        var offset = this.stackSlots.get(virtReg);
        newList.add(new MovInstruction(virtReg.getWidth(), new MemoryLocation(HardwareRegister.RBP, offset), value));
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

    private HardwareRegister initialiseVirtualRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        var pref = Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of);
        return this.initialiseVirtualRegister(virtReg, Optional.empty(), pref, List.of(), a, newList);
    }

    private HardwareRegister initialiseVirtualRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive, Optional<HardwareRegister> dontChoose, Set<HardwareRegister> hint) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        List<HardwareRegister.Group> pref = Stream.concat(Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of).stream(), hint.stream().map(HardwareRegister::getGroup)).toList();
        return this.initialiseVirtualRegister(virtReg, Optional.empty(), pref, dontChoose.stream().map(HardwareRegister::getGroup).toList(), a, newList);
    }

    private HardwareRegister concretizeRegister(VirtualRegister virtReg, List<Instruction> newList, Set<HardwareRegister> keepAlive) {
        Set<HardwareRegister.Group> a = keepAlive.stream().map(HardwareRegister::getGroup).collect(Collectors.toSet());
        var pref = Optional.ofNullable(this.registerHints.get(virtReg)).orElseGet(List::of);
        return this.concretizeVirtualRegister(virtReg, Optional.empty(), pref, List.of(), a, newList);
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
            var hardwareIndexReg = this.concretizeRegister(virtReg, newList, except);
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
        deadRegs.forEach(this.freeRegisters::freeMapping);
    }

    private void allocateRegForInstruction(Instruction instr, Set<VirtualRegister> liveRegs, List<Instruction> newList) {
        switch (instr) {
            case DivInstruction div -> {
                var dividendVirtReg = (VirtualRegister) div.getDividend();
                var divisorVirtReg = (VirtualRegister) div.getDivisor();

                var divisorHardwareReg = this.concretizeRegister(divisorVirtReg, newList, Set.of());

                HardwareRegister dividendHardwareReg;
                if (!liveRegs.contains(dividendVirtReg)) {
                    // We can load dividend directly into EAX because it is dead after this instruction.
                    dividendHardwareReg = this.concretizeRegisterInto(HardwareRegister.EAX, dividendVirtReg, newList);
                } else {
                    dividendHardwareReg = this.concretizeRegister(dividendVirtReg, newList, Set.of(divisorHardwareReg));
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

                this.freeRegisters.createSpecificMapping(targetVirtReg, targetHardwareReg);

                div.setTarget(targetHardwareReg);
                div.setDividend(dividendHardwareReg);
                div.setDivisor(divisorHardwareReg);

            }
            case BinaryInstruction binary -> {
                var usedRegisters = new HashSet<HardwareRegister>();

                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsVirtReg = (VirtualRegister) binary.getLhs();

                var lhsReg= this.concretizeRegister(lhsVirtReg, newList, Set.of());
                usedRegisters.add(lhsReg);

                Optional<HardwareRegister> mustStayLive = Optional.empty();
                if (binary.getRhs() instanceof MemoryLocation rhs) {
                    usedRegisters.addAll(this.concretizeMemoryLocation(rhs, newList, usedRegisters));
                } else if (binary.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList, usedRegisters);
                    mustStayLive = Optional.of(rhsHardwareReg);
                    usedRegisters.add(rhsHardwareReg);
                    binary.setRhs(rhsHardwareReg);
                }

                // If the rhs register is different from the lhs register, it is important that the target hardware register
                // doesn't allocate the same hardware register as rhs (which might be dead), because it would get overwritten.
                // However, if both operands are the same there this is allowed.
                if (mustStayLive.isPresent() && lhsReg.equals(mustStayLive.get())) {
                    mustStayLive = Optional.empty();
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
                this.freeAllRegisters(newList);
                newList.add(jump);
            }
            case CmpInstruction cmp -> {
                var lhsVirtReg = (VirtualRegister) cmp.getLhs();
                var lhsHardwareReg = this.concretizeRegister(lhsVirtReg, newList, Set.of());
                var rhsVirtReg = (VirtualRegister) cmp.getRhs();
                var rhsHardwareReg = this.concretizeRegister(rhsVirtReg, newList, Set.of(lhsHardwareReg));

                this.freeDeadVirtualRegisters(liveRegs);

                cmp.setLhs(lhsHardwareReg);
                cmp.setRhs(rhsHardwareReg);
                newList.add(cmp);
            }
            case BranchInstruction branch -> {
                this.freeAllRegisters(newList);
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

                allocCall.setTarget(HardwareRegister.RAX);

                this.freeRegisters.getOrCreateSpecificMapping(virtRegTarget, HardwareRegister.RAX).orElseThrow();
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
                            methodCall.setTarget(HardwareRegister.EAX);
                            this.freeRegisters.getOrCreateSpecificMapping(targetVirtReg, HardwareRegister.EAX);
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

                            var targetHardwareReg = HardwareRegister.RAX.forWidth(targetVirtReg.getWidth());
                            methodCall.setTarget(targetHardwareReg);
                            this.freeRegisters.getOrCreateSpecificMapping(targetVirtReg, targetHardwareReg);
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
                        // We hint that we want to use the same register as the source, to increase chance that this move will have the same source and
                        // destination, so that the peephole optimizer can remove this move.
                        var targetHardwareReg = this.initialiseVirtualRegister(virtualReg, newList, usedRegisters, Optional.empty(), usedRegisters);
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
            default -> throw new AssertionError("These instructions should not appear pre register allocation");
        }
    }

    private void allocateBasicBlock(BasicBlock bb) {
        List<Instruction> newList = new ArrayList<>();

        // Globber all registers
        this.freeRegisters.clearAllMappings();

        // add function prolog
        if (bb == this.graph.getStartBlock()) {
            newList.add(new PushInstruction(HardwareRegister.RBP));
            newList.add(new MovInstruction(Register.Width.BIT64, HardwareRegister.RBP, HardwareRegister.RSP));
            var stackSpace = new SubInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(0));
            this.allocateStackSpaceInstruction = Optional.of(stackSpace);
            newList.add(stackSpace);
        }

        for (int i = 0; i < bb.getInstructions().size(); i++) {
            var instruction = bb.getInstructions().get(i);
            var liveRegisters = this.lifetimes.getLiveRegisters(bb, i);
            this.allocateRegForInstruction(instruction, liveRegisters, newList);
        }

        bb.setInstructions(newList);
    }

    private void scheduleBlocks() {
        HashSet<BasicBlock> visitedBlocks = new HashSet<>();

        this.graph.getBlocks().clear();

        this.scheduleBlockDFS(this.graph.getStartBlock(), visitedBlocks, this.graph.getBlocks());
    }

    private void scheduleBlockDFS(BasicBlock bb, HashSet<BasicBlock> visited, List<BasicBlock> blockSequence) {
        if (visited.contains(bb)) {
            return;
        } else {
            visited.add(bb);
            blockSequence.add(bb);
        }

        var lastInstr = (ControlFlowInstruction) bb.getInstructions().get(bb.getInstructions().size() - 1);
        switch (lastInstr) {
            case JumpInstruction jump -> this.scheduleBlockDFS(jump.getTarget(), visited, blockSequence);
            case BranchInstruction branch -> {
                this.scheduleBlockDFS(branch.getFalseBlock(), visited, blockSequence);
                this.scheduleBlockDFS(branch.getTrueBlock(), visited, blockSequence);
            }
            case ReturnInstruction ignored -> {}
        }
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