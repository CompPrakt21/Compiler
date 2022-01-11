package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NaiveRegisterAllocator {

    private final SirGraph graph;
    private final StackSlots stackSlots;
    private final FreeRegisterManager freeRegisters;
    private final List<VirtualRegister> methodParameters;

    /**
     * The sub instruction which allocates stack space for local variables.
     * After register allocation is finished (and all required virtual registers are spilled)
     * we know how much stackspace we need and update this value.
     * The same is true for when we free the allocated stack space before ret instructions.
     */
    private Optional<SubInstruction> allocateStackSpaceInstruction;
    private final List<AddInstruction> freeStackSpaceInstructions;

    public NaiveRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph) {
        this.methodParameters = methodParameters;
        this.graph = graph;
        this.allocateStackSpaceInstruction = Optional.empty();
        this.freeStackSpaceInstructions = new ArrayList<>();
        this.stackSlots = new StackSlots();
        this.freeRegisters = new FreeRegisterManager();
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

    private HardwareRegister concretizeRegister(Register reg, List<Instruction> newList) {
        var virtReg = (VirtualRegister) reg;
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.requestRegister(virtReg.getWidth());
        newList.add(new MovInstruction(reg.getWidth(), hardwareReg, new MemoryLocation(HardwareRegister.RBP, offset)));
        return hardwareReg;
    }

    private HardwareRegister concretizeRegisterInto(HardwareRegister target, VirtualRegister virtReg, List<Instruction> newList) {
        assert target.getWidth() == virtReg.getWidth();
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.requestSpecificRegister(target).orElseThrow();

        newList.add(new MovInstruction(target.getWidth(), hardwareReg, new MemoryLocation(HardwareRegister.RBP, offset)));

        return hardwareReg;
    }

    private void saveVirtualRegister(VirtualRegister virtReg, HardwareRegister value, List<Instruction> newList)  {
        var offset = this.stackSlots.get(virtReg);
        newList.add(new MovInstruction(virtReg.getWidth(), new MemoryLocation(HardwareRegister.RBP, offset), value));
    }

    /**
     * replaces virtual registers in a memory loation with free hardware registers and adds the necessary load
     * instructions.
     */
    private List<HardwareRegister> concretizeMemoryLocation(MemoryLocation loc, List<Instruction> newList) {
        List<HardwareRegister> allocatedRegisters = new ArrayList<>();

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

        return allocatedRegisters;
    }

    private void allocateRegForInstruction(Instruction instr, List<Instruction> newList) {
        switch (instr) {
            case DivInstruction div -> {
                var dividendVirtReg = (VirtualRegister) div.getDividend();
                var divisorVirtReg = (VirtualRegister) div.getDivisor();

                var implicitLowerDividendReg = this.concretizeRegisterInto(HardwareRegister.EAX, dividendVirtReg, newList);
                var implicitUpperDividendReg = this.freeRegisters.requestSpecificRegister(HardwareRegister.EDX).orElseThrow();

                newList.add(new ConvertDoubleToQuadInstruction(implicitUpperDividendReg, implicitLowerDividendReg));

                var divisorHardwareReg= this.concretizeRegister(divisorVirtReg, newList);

                var targetVirtReg = (VirtualRegister) div.getTarget();

                div.setDividend(implicitLowerDividendReg);
                div.setDivisor(divisorHardwareReg);

                switch (div.getType()) {
                    case Div -> div.setTarget(implicitLowerDividendReg);
                    case Mod -> div.setTarget(implicitUpperDividendReg);
                }

                newList.add(div);

                this.saveVirtualRegister(targetVirtReg, (HardwareRegister) div.getTarget(), newList);

                this.freeRegisters.releaseRegister(implicitLowerDividendReg);
                this.freeRegisters.releaseRegister(implicitUpperDividendReg);
                this.freeRegisters.releaseRegister(divisorHardwareReg);
            }
            case BinaryInstruction binary -> {
                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsReg= this.concretizeRegister(binary.getLhs(), newList);

                var allocatedRegisters = new ArrayList<HardwareRegister>();

                if (binary.getRhs() instanceof MemoryLocation rhs) {
                    var memRegs = this.concretizeMemoryLocation(rhs, newList);
                    allocatedRegisters.addAll(memRegs);
                } else if (binary.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList);
                    binary.setRhs(rhsHardwareReg);
                    allocatedRegisters.add(rhsHardwareReg);
                }

                binary.setTarget(lhsReg);
                binary.setLhs(lhsReg);

                newList.add(binary);

                this.saveVirtualRegister(targetReg, lhsReg, newList);

                this.freeRegisters.releaseRegister(lhsReg);
                allocatedRegisters.forEach(this.freeRegisters::releaseRegister);
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
                        for (int i = methodCall.getArguments().size() - 1; i >= 0; i--) {
                            var hardwareReg = this.concretizeRegister(methodCall.getArguments().get(i), newList);

                            newList.add(new PushInstruction(hardwareReg.forWidth(Register.Width.BIT64)));

                            this.freeRegisters.releaseRegister(hardwareReg);
                        }
                        var requiredStackSpace = methodCall.getArguments().size() * Register.Width.BIT64.getByteSize();

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
            case MovInstruction mov -> {
                ArrayList<HardwareRegister> allocatedRegisters = new ArrayList<>();

                if (mov.getSource() instanceof VirtualRegister source) {
                    var sourceHardwareReg = this.concretizeRegister(source, newList);
                    allocatedRegisters.add(sourceHardwareReg);
                    mov.setSource(sourceHardwareReg);
                } else if (mov.getSource() instanceof MemoryLocation source) {
                    allocatedRegisters.addAll(this.concretizeMemoryLocation(source, newList));
                }

                switch (mov.getDestination()) {
                    case MemoryLocation loc -> {
                        allocatedRegisters.addAll(this.concretizeMemoryLocation(loc, newList));
                        newList.add(mov);
                    }
                    case VirtualRegister virtualReg -> {
                        var targetHardwareReg = this.freeRegisters.requestRegister(mov.getWidth());
                        mov.setDestination(targetHardwareReg);
                        newList.add(mov);
                        allocatedRegisters.add(targetHardwareReg);
                        this.saveVirtualRegister(virtualReg, targetHardwareReg, newList);
                    }
                    default -> throw new AssertionError("Unexpected mov destination");
                }

                allocatedRegisters.forEach(this.freeRegisters::releaseRegister);
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
            default -> throw new AssertionError("These instructions should not appear pre register allocation");
        }
    }

    private void allocateBasicBlock(BasicBlock bb) {
        List<Instruction> newList = new ArrayList<>();

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
