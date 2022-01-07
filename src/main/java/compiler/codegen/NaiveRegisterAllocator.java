package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NaiveRegisterAllocator {

    private SirGraph graph;
    private StackSlots stackSlots;
    private FreeRegisterManager freeRegisters;
    private List<VirtualRegister> methodParameters;

    /**
     * The sub instruction which allocates stack space for local variables.
     * After register allocation is finished (and all required virtual registers are spilled)
     * we know how much stackspace we need and update this value.
     * The same is true for when we free the allocated stack space before ret instructions.
     */
    private Optional<SubInstruction> allocateStackSpaceInstruction;
    private List<AddInstruction> freeStackSpaceInstructions;
    /**
     * These move store instructions copy function parameters onto the activation record of a
     * called function. The offset is relative of the calling functions rbp and is the sum
     * of the final stack slot offset and argument offset.
     * We need to add the final stack slot offset at the end.
     */
    private List<MovStoreInstruction> copyFunctionParams;

    public NaiveRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph) {
        this.methodParameters = methodParameters;
        this.graph = graph;
        this.allocateStackSpaceInstruction = Optional.empty();
        this.freeStackSpaceInstructions = new ArrayList<>();
        this.copyFunctionParams = new ArrayList<>();
        this.stackSlots = new StackSlots();
        this.freeRegisters = new FreeRegisterManager();
    }

    /**
     * Every virtual register is replaced with a hardware register.
     * Mutates the graph and schedule if necessary.
     */
    public void allocate() {
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

        this.copyFunctionParams.forEach(movInstr -> {
            var memLoc = movInstr.getAddress();
            memLoc.setConstant(memLoc.getConstant() + stackOffset);
        });

        this.scheduleBlocks();
    }

    private HardwareRegister concretizeRegister(Register reg, List<Instruction> newList) {
        var virtReg = (VirtualRegister) reg;
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.requestRegister(virtReg.getWidth());
        newList.add(new MovLoadInstruction(hardwareReg, new MemoryLocation(HardwareRegister.RBP, offset)));
        return hardwareReg;
    }

    private HardwareRegister concretizeRegisterInto(HardwareRegister target, VirtualRegister virtReg, List<Instruction> newList) {
        assert target.getWidth() == virtReg.getWidth();
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.requestSpecificRegister(target).orElseThrow();

        newList.add(new MovLoadInstruction(hardwareReg, new MemoryLocation(HardwareRegister.RBP, offset)));

        return hardwareReg;
    }

    private void saveVirtualRegister(VirtualRegister virtReg, HardwareRegister value, List<Instruction> newList)  {
        var offset = this.stackSlots.get(virtReg);
        newList.add(new MovStoreInstruction(new MemoryLocation(HardwareRegister.RBP, offset), value));
    }

    /**
     * replaces virtual registers in a memory loation with free hardware registers and adds the necessary load
     * instructions.
     */
    private List<HardwareRegister> concretizeOperand(Operand operand, List<Instruction> newList) {
        List<HardwareRegister> allocatedRegisters = new ArrayList<>();

        switch (operand) {
            case MemoryLocation loc -> {

                if (loc.getBaseRegister().isPresent() && loc.getBaseRegister().get() instanceof VirtualRegister virtReg) {
                    var hardwareBaseReg = this.concretizeRegister(virtReg, newList);
                    loc.setBaseRegister(hardwareBaseReg);
                    allocatedRegisters.add(hardwareBaseReg);
                }

                if (loc.getIndex().isPresent() && loc.getIndex().get() instanceof VirtualRegister virtReg) {
                    var hardwareIndexReg = this.concretizeRegister(virtReg, newList);
                    loc.setIndex(hardwareIndexReg);
                    allocatedRegisters.add(hardwareIndexReg);
                }
            }
            case VirtualRegister virtReg ->
                allocatedRegisters.add(this.concretizeRegister(virtReg, newList));
            case HardwareRegister ignored -> {}
            case Constant ignored -> {}
        }

        return allocatedRegisters;
    }

    private void allocateRegForInstruction(Instruction instr, List<Instruction> newList) {
        switch (instr) {
            case DivInstruction div -> {
                var lhsVirtReg = (VirtualRegister) div.getLhs();
                var rhsVirtReg = (VirtualRegister) div.getRhs();
                var lhs = this.concretizeRegisterInto(HardwareRegister.EAX, lhsVirtReg, newList);
                var rhs = this.concretizeRegisterInto(HardwareRegister.EDX, rhsVirtReg, newList);

                div.setLhs(lhs);
                div.setRhs(rhs);

                var targetVirtReg = (VirtualRegister)div.getTarget();

                switch (div.getType()) {
                    case Div -> div.setTarget(lhs);
                    case Mod -> div.setTarget(rhs);
                }

                newList.add(div);

                this.saveVirtualRegister(targetVirtReg, (HardwareRegister) div.getTarget(), newList);

                this.freeRegisters.releaseRegister(lhs);
                this.freeRegisters.releaseRegister(rhs);
            }
            case BinaryInstruction binary -> {
                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsReg= this.concretizeRegister(binary.getLhs(), newList);
                var memRegs = this.concretizeOperand(binary.getRhs(), newList);

                // if the operand is a memory location we replace virtual register inplace.
                // otherwise we need to change it manually.
                if (binary.getRhs() instanceof VirtualRegister) {
                    binary.setRhs(memRegs.get(0));
                }

                binary.setTarget(lhsReg);
                binary.setLhs(lhsReg);

                newList.add(binary);

                this.saveVirtualRegister(targetReg, lhsReg, newList);

                this.freeRegisters.releaseRegister(lhsReg);
                memRegs.forEach(reg -> this.freeRegisters.releaseRegister(reg));
            }
            case ReturnInstruction ret -> {
                // Load return value into RAX/EAX
                if (ret.getReturnValue().isPresent()) {
                    var retVirtReg = (VirtualRegister)ret.getReturnValue().get();
                    var hardwareReg = this.concretizeRegisterInto(HardwareRegister.EAX.forWidth(retVirtReg.getWidth()), retVirtReg, newList);
                    ret.setReturnValue(hardwareReg);
                    this.freeRegisters.releaseRegister(hardwareReg);
                }

                // Function epilog
                var freeStackSpace = new AddInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(0));
                this.freeStackSpaceInstructions.add(freeStackSpace);

                newList.add(freeStackSpace);
                newList.add(new LeaveInstruction());
                newList.add(ret);
            }
            case JumpInstruction jump -> newList.add(jump);
            case CmpInstruction cmp -> {
                var lhsHardwareReg = this.concretizeRegister(cmp.getLhs(), newList);
                var rhsHardwareReg = this.concretizeRegister(cmp.getRhs(), newList);
                cmp.setLhs(lhsHardwareReg);
                cmp.setRhs(rhsHardwareReg);
                newList.add(cmp);
                this.freeRegisters.releaseRegister(lhsHardwareReg);
                this.freeRegisters.releaseRegister(rhsHardwareReg);
            }
            case BranchInstruction branch -> newList.add(branch);
            case AllocCallInstruction allocCall -> {
                var objectSizeReg = this.concretizeRegisterInto(HardwareRegister.EDI, (VirtualRegister) allocCall.getObjectSize(), newList);
                var numElementsReg = this.concretizeRegisterInto(HardwareRegister.ESI, (VirtualRegister) allocCall.getNumElements(), newList);

                var virtRegTarget = (VirtualRegister) allocCall.getTarget();

                allocCall.setNumElements(numElementsReg);
                allocCall.setObjectSize(objectSizeReg);

                newList.add(allocCall);

                allocCall.setTarget(HardwareRegister.RAX);

                this.saveVirtualRegister(virtRegTarget, HardwareRegister.RAX, newList);

                this.freeRegisters.releaseRegister(objectSizeReg);
                this.freeRegisters.releaseRegister(numElementsReg);
            }
            case MethodCallInstruction methodCall -> {
                switch (methodCall.getMethod()) {
                    case IntrinsicMethod intrinsic -> {
                        assert methodCall.getArguments().size() <= 1;

                        // Intrinsic has an argument, so we load it into edi
                        if (methodCall.getArguments().size() == 1) {
                            var argReg= this.concretizeRegisterInto(HardwareRegister.EDI, (VirtualRegister) methodCall.getArguments().get(0), newList);
                            methodCall.getArguments().set(0, argReg);
                            this.freeRegisters.releaseRegister(argReg);
                        }

                        newList.add(methodCall);

                        if (!(intrinsic.getReturnTy() instanceof VoidTy)) {
                            var targetVirtReg = (VirtualRegister)methodCall.getTarget();
                            this.saveVirtualRegister(targetVirtReg, HardwareRegister.EAX, newList);
                            methodCall.setTarget(HardwareRegister.EAX);
                        }
                    }
                    case DefinedMethod method -> {
                        // reserve stack space
                        var requiredStackSpace = methodCall.getArguments().size() * Register.Width.BIT64.getByteSize();
                        newList.add(new SubInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(requiredStackSpace)));

                        // copy params onto stack space
                        for (int i = 0; i < methodCall.getArguments().size(); i++) {
                            var hardwareReg = this.concretizeRegister(methodCall.getArguments().get(i), newList);

                            var paramOffset = - (i + 1) * Register.Width.BIT64.getByteSize();
                            var paramStoreInstr = new MovStoreInstruction(new MemoryLocation(HardwareRegister.RBP, paramOffset), hardwareReg);
                            this.copyFunctionParams.add(paramStoreInstr);
                            newList.add(paramStoreInstr);

                            this.freeRegisters.releaseRegister(hardwareReg);
                        }

                        // call function
                        newList.add(methodCall);

                        // save return value;
                        if (!(method.getReturnTy() instanceof VoidTy)) {
                            var targetVirtReg = (VirtualRegister) methodCall.getTarget();
                            var targetHardwareReg = HardwareRegister.RAX.forWidth(targetVirtReg.getWidth());
                            this.saveVirtualRegister(targetVirtReg, targetHardwareReg, newList);
                            methodCall.setTarget(targetHardwareReg);
                        }

                        // free stack space
                        newList.add(new AddInstruction(HardwareRegister.RSP, HardwareRegister.RSP, new Constant(requiredStackSpace)));
                    }
                }
            }
            case MovImmediateInstruction movImm -> {
                var targetVirtReg = (VirtualRegister) movImm.getTarget();
                var targetHardwareReg = this.freeRegisters.requestRegister(targetVirtReg.getWidth());
                movImm.setTarget(targetHardwareReg);
                newList.add(movImm);
                this.saveVirtualRegister(targetVirtReg, targetHardwareReg, newList);
                this.freeRegisters.releaseRegister(targetHardwareReg);
            }
            case MovRegInstruction movReg -> {
                if (movReg.getSource() instanceof VirtualRegister virtRegSource) {
                    var sourceHardwareReg = this.concretizeRegister(virtRegSource, newList);
                    movReg.setSource(sourceHardwareReg);
                    this.freeRegisters.releaseRegister(sourceHardwareReg);
                }

                newList.add(movReg);

                if (movReg.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.freeRegisters.requestRegister(virRegTarget.getWidth());
                    this.saveVirtualRegister(virRegTarget, (HardwareRegister) movReg.getSource(), newList);
                    movReg.setTarget(targetHardware);
                    this.freeRegisters.releaseRegister(targetHardware);
                }
            }
            case MovSignExtendInstruction movSX -> {
                if (movSX.getInput() instanceof VirtualRegister virtRegInput) {
                    var inputHardwareReg = this.concretizeRegister(virtRegInput, newList);
                    movSX.setInput(inputHardwareReg);
                    this.freeRegisters.releaseRegister(inputHardwareReg);
                }

                newList.add(movSX);

                if (movSX.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.freeRegisters.requestRegister(virRegTarget.getWidth());
                    this.saveVirtualRegister(virRegTarget, targetHardware, newList);
                    movSX.setTarget(targetHardware);
                    this.freeRegisters.releaseRegister(targetHardware);
                }
            }
            case MovStoreInstruction movStore -> {
                var memRegs = this.concretizeOperand(movStore.getAddress(), newList);

                var valueReg = (VirtualRegister)movStore.getValue();
                var valueHardwareReg = this.concretizeRegister(valueReg, newList);

                movStore.setValue(valueHardwareReg);

                newList.add(movStore);

                this.freeRegisters.releaseRegister(valueHardwareReg);
                memRegs.forEach(reg -> this.freeRegisters.releaseRegister(reg));
            }
            case MovLoadInstruction movLoad -> {
                var memRegs = this.concretizeOperand(movLoad.getAddress(), newList);

                var targetVirtReg = (VirtualRegister)movLoad.getTarget();

                var resultHardwareReg = this.freeRegisters.requestRegister(targetVirtReg.getWidth());

                movLoad.setTarget(resultHardwareReg);
                newList.add(movLoad);

                this.saveVirtualRegister(targetVirtReg, resultHardwareReg, newList);

                this.freeRegisters.releaseRegister(resultHardwareReg);
                memRegs.forEach(reg -> this.freeRegisters.releaseRegister(reg));
            }
            default -> throw new AssertionError("These instructions should not appear pre register allocation");
        }
    }

    private void allocateBasicBlock(BasicBlock bb) {
        List<Instruction> newList = new ArrayList<>();

        // add function prolog
        if (bb == this.graph.getStartBlock()) {
            newList.add(new PushInstruction(HardwareRegister.RBP));
            newList.add(new MovRegInstruction(HardwareRegister.RBP, HardwareRegister.RSP));
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
