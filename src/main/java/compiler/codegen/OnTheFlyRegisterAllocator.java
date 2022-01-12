package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

import java.util.*;

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

    public OnTheFlyRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph) {
        this.methodParameters = methodParameters;
        this.graph = graph;
        this.allocateStackSpaceInstruction = Optional.empty();
        this.freeStackSpaceInstructions = new ArrayList<>();
        this.stackSlots = new StackSlots();
        this.freeRegisters = new RegisterManager();
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

    /**
     * Stores a mapped variable on the stack to free a hardware register.
     *
     * @param except The set of virtual registers that need to remain live.
     */
    private void reduceRegisterPressure(List<Instruction> newList, Set<VirtualRegister> except) {
        var removedReg= this.freeRegisters.getMapping().leftSet().stream().filter(virtReg -> !except.contains(virtReg)).findFirst().orElseThrow();
        this.makeUnusedSpecificRegister(this.freeRegisters.getMapping(removedReg).orElseThrow(), newList);
    }

    private void makeUnusedSpecificRegister(HardwareRegister register, List<Instruction> newList) {
        if (!this.freeRegisters.isAvailable(register)) {
            var associatedVirtRegister = this.freeRegisters.getMappedVirtualRegister(register.getGroup());
            this.saveVirtualRegister(associatedVirtRegister, register.getGroup().getRegister(associatedVirtRegister.getWidth()), newList);
            this.freeRegisters.freeMapping(associatedVirtRegister);
        }
    }

    private void freeAllRegisters(List<Instruction> newList, Set<VirtualRegister> except) {
        List<VirtualRegister> regsToBeFreed = new ArrayList<>();
        for (var virtReg : this.freeRegisters.getMapping().leftSet()) {
            var hardwareReg = this.freeRegisters.getMapping(virtReg).orElseThrow();

            this.saveVirtualRegister(virtReg, hardwareReg, newList);

            if (!except.contains(virtReg)) {
                regsToBeFreed.add(virtReg);
            }
        }

        regsToBeFreed.forEach(this.freeRegisters::freeMapping);
    }

    private HardwareRegister initialiseVirtualRegister(VirtualRegister virtReg, List<Instruction> newList, Set<VirtualRegister> except) {
        var hardwareReg = this.freeRegisters.getOrCreateMapping(virtReg);

        if (hardwareReg.isPresent()) {
            return hardwareReg.get();
        } else {
            this.reduceRegisterPressure(newList, except);
            return this.freeRegisters.getOrCreateMapping(virtReg).orElseThrow();
        }
    }

    private HardwareRegister concretizeRegister(VirtualRegister virtReg, List<Instruction> newList, Set<VirtualRegister> except) {
        var hasMapping = this.freeRegisters.getMapping(virtReg);

        if (hasMapping.isPresent()) {
            return hasMapping.get();
        } else {

            var hardwareReg = this.freeRegisters.createMapping(virtReg);
            if (hardwareReg.isEmpty()) {
                this.reduceRegisterPressure(newList, except);
                hardwareReg = this.freeRegisters.createMapping(virtReg);
            }

            var reg = hardwareReg.orElseThrow();
            var offset = this.stackSlots.get(virtReg);
            newList.add(new MovInstruction(reg.getWidth(), reg, new MemoryLocation(HardwareRegister.RBP, offset)));
            return reg;
        }
    }

    private HardwareRegister concretizeRegisterInto(HardwareRegister target, VirtualRegister virtReg, List<Instruction> newList) {
        assert target.getWidth() == virtReg.getWidth();

        var hasMapping = this.freeRegisters.getMapping(virtReg);

        // virtual register is already mapped to the correct hardware register.
        if (hasMapping.isPresent() && hasMapping.get().equals(target)) {
            return target;
        }


        // Make sure target hardware register is free to be used
        this.makeUnusedSpecificRegister(target, newList);

        if (hasMapping.isPresent()) {
            // if register is mapped to different virtual register a simple register move suffices
            newList.add(new MovInstruction(virtReg.getWidth(), target, hasMapping.get()));
            this.freeRegisters.freeHardwareRegister(hasMapping.get());
        } else {
            var offset = this.stackSlots.get(virtReg);
            newList.add(new MovInstruction(virtReg.getWidth(), target, new MemoryLocation(HardwareRegister.RBP, offset)));
        }

        // In this case it will always create mapping
        this.freeRegisters.getOrCreateSpecificMapping(virtReg, target).orElseThrow();

        return target;
    }

    private void saveVirtualRegister(VirtualRegister virtReg, HardwareRegister value, List<Instruction> newList)  {
        var offset = this.stackSlots.get(virtReg);
        newList.add(new MovInstruction(virtReg.getWidth(), new MemoryLocation(HardwareRegister.RBP, offset), value));
    }

    /**
     * replaces virtual registers in a memory loation with free hardware registers and adds the necessary load
     * instructions.
     */
    private List<VirtualRegister> concretizeMemoryLocation(MemoryLocation loc, List<Instruction> newList, Set<VirtualRegister> except) {
        List<VirtualRegister> mappings = new ArrayList<>();

        if (loc.getBaseRegister().isPresent() && loc.getBaseRegister().get() instanceof VirtualRegister virtReg) {
            var hardwareBaseReg = this.concretizeRegister(virtReg, newList, except);
            loc.setBaseRegister(hardwareBaseReg);
            mappings.add(virtReg);
        }

        if (loc.getIndex().isPresent() && loc.getIndex().get() instanceof VirtualRegister virtReg) {
            var hardwareIndexReg = this.concretizeRegister(virtReg, newList, except);
            loc.setIndex(hardwareIndexReg);
            mappings.add(virtReg);
        }

        return mappings;
    }

    private void allocateRegForInstruction(Instruction instr, List<Instruction> newList) {
        switch (instr) {
            case DivInstruction div -> {
                var dividendVirtReg = (VirtualRegister) div.getDividend();
                var divisorVirtReg = (VirtualRegister) div.getDivisor();

                this.makeUnusedSpecificRegister(HardwareRegister.EAX, newList);
                this.makeUnusedSpecificRegister(HardwareRegister.EDX, newList);

                var implicitLowerDividendReg = this.freeRegisters.getHardwareRegister(HardwareRegister.EAX).orElseThrow();
                var implicitUpperDividendReg = this.freeRegisters.getHardwareRegister(HardwareRegister.EDX).orElseThrow();

                // The except set can be empty because implicitLowerDividendReg and implicitUpperDividendReg are not map, just taken out the freelist.
                var dividendHardwareReg = this.concretizeRegister(dividendVirtReg, newList, Set.of());
                var divisorHardwareReg= this.concretizeRegister(divisorVirtReg, newList, Set.of());

                newList.add(new MovInstruction(implicitLowerDividendReg.getWidth(), implicitLowerDividendReg, dividendHardwareReg));
                newList.add(new ConvertDoubleToQuadInstruction(implicitUpperDividendReg, implicitLowerDividendReg));

                var targetVirtReg = (VirtualRegister) div.getTarget();

                div.setDividend(implicitLowerDividendReg);
                div.setDivisor(divisorHardwareReg);

                switch (div.getType()) {
                    case Div -> {
                        div.setTarget(implicitLowerDividendReg);
                        this.freeRegisters.freeHardwareRegister(HardwareRegister.EAX);
                        this.freeRegisters.getOrCreateSpecificMapping(targetVirtReg, implicitLowerDividendReg).orElseThrow();
                        this.freeRegisters.freeHardwareRegister(HardwareRegister.EDX);
                    }
                    case Mod -> {
                        div.setTarget(implicitUpperDividendReg);
                        this.freeRegisters.freeHardwareRegister(HardwareRegister.EDX);
                        this.freeRegisters.getOrCreateSpecificMapping(targetVirtReg, implicitUpperDividendReg).orElseThrow();
                        this.freeRegisters.freeHardwareRegister(HardwareRegister.EAX);
                    }
                }

                newList.add(div);
            }
            case BinaryInstruction binary -> {
                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsVirtReg = (VirtualRegister) binary.getLhs();

                var lhsReg= this.concretizeRegister(lhsVirtReg, newList, Set.of());

                var targetHardwareReg = this.initialiseVirtualRegister(targetReg, newList, Set.of(lhsVirtReg));
                newList.add(new MovInstruction(targetHardwareReg.getWidth(), targetHardwareReg, lhsReg));

                if (binary.getRhs() instanceof MemoryLocation rhs) {
                    this.concretizeMemoryLocation(rhs, newList, Set.of(lhsVirtReg, targetReg));
                } else if (binary.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList, Set.of(lhsVirtReg, targetReg));
                    assert targetHardwareReg.getGroup() != rhsHardwareReg.getGroup();
                    binary.setRhs(rhsHardwareReg);
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

                // Function epilog
                var freeStackSpace = new AddInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(0));
                this.freeStackSpaceInstructions.add(freeStackSpace);

                newList.add(freeStackSpace);
                newList.add(new LeaveInstruction());
                newList.add(ret);
            }
            case JumpInstruction jump -> {
                this.freeAllRegisters(newList, Set.of());
                newList.add(jump);
            }
            case CmpInstruction cmp -> {
                var lhsVirtReg = (VirtualRegister) cmp.getLhs();
                var lhsHardwareReg = this.concretizeRegister(lhsVirtReg, newList, Set.of());
                var rhsVirtReg = (VirtualRegister) cmp.getRhs();
                var rhsHardwareReg = this.concretizeRegister(rhsVirtReg, newList, Set.of(lhsVirtReg));

                cmp.setLhs(lhsHardwareReg);
                cmp.setRhs(rhsHardwareReg);
                newList.add(cmp);
            }
            case BranchInstruction branch -> {
                this.freeAllRegisters(newList, Set.of());
                newList.add(branch);
            }
            case AllocCallInstruction allocCall -> {
                var objectSizeVirtReg = (VirtualRegister) allocCall.getObjectSize();
                var objectSizeReg = this.concretizeRegisterInto(HardwareRegister.EDI, objectSizeVirtReg, newList);
                var numElementsVirtReg = (VirtualRegister) allocCall.getNumElements();
                var numElementsReg = this.concretizeRegisterInto(HardwareRegister.ESI, numElementsVirtReg, newList);

                this.freeAllRegisters(newList, Set.of(objectSizeVirtReg, numElementsVirtReg));

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
                            this.freeAllRegisters(newList, Set.of(argVirtReg));
                        } else {
                            this.freeAllRegisters(newList, Set.of());
                        }

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

                        this.freeAllRegisters(newList, Set.of());

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
                var usedRegisters = new HashSet<VirtualRegister>();

                if (mov.getSource() instanceof VirtualRegister source) {
                    var sourceHardwareReg = this.concretizeRegister(source, newList, usedRegisters);
                    usedRegisters.add(source);
                    mov.setSource(sourceHardwareReg);
                } else if (mov.getSource() instanceof MemoryLocation source) {
                    usedRegisters.addAll(this.concretizeMemoryLocation(source, newList, usedRegisters));
                }

                switch (mov.getDestination()) {
                    case MemoryLocation loc -> {
                        usedRegisters.addAll(this.concretizeMemoryLocation(loc, newList, usedRegisters));
                        newList.add(mov);
                    }
                    case VirtualRegister virtualReg -> {
                        var targetHardwareReg = this.initialiseVirtualRegister(virtualReg, newList, usedRegisters);
                        usedRegisters.add(virtualReg);
                        mov.setDestination(targetHardwareReg);
                        newList.add(mov);
                    }
                    default -> throw new AssertionError("Unexpected mov destination");
                }
            }
            case MovSignExtendInstruction movSX -> {
                var usedRegisters = new HashSet<VirtualRegister>();

                if (movSX.getInput() instanceof VirtualRegister virtRegInput) {
                    var inputHardwareReg = this.concretizeRegister(virtRegInput, newList, usedRegisters);
                    usedRegisters.add(virtRegInput);
                    movSX.setInput(inputHardwareReg);
                }

                if (movSX.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.initialiseVirtualRegister(virRegTarget, newList, usedRegisters);
                    usedRegisters.add(virRegTarget);
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

        for (var instruction : bb.getInstructions()) {
            this.allocateRegForInstruction(instruction, newList);
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
}