package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.IntrinsicMethod;
import compiler.types.VoidTy;

import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class NaiveRegisterAllocator {

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

    public NaiveRegisterAllocator(List<VirtualRegister> methodParameters, SirGraph graph) {
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

    private HardwareRegister concretizeRegister(VirtualRegister virtReg, List<Instruction> newList) {
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.getOrCreateMapping(virtReg).orElseThrow();
        newList.add(new MovInstruction(hardwareReg.getWidth(), hardwareReg, new MemoryLocation(HardwareRegister.RBP, offset)));
        return hardwareReg;
    }

    private HardwareRegister concretizeRegisterInto(HardwareRegister target, VirtualRegister virtReg, List<Instruction> newList) {
        assert target.getWidth() == virtReg.getWidth();
        var offset = this.stackSlots.get(virtReg);
        var hardwareReg = this.freeRegisters.getOrCreateSpecificMapping(virtReg, target).orElseThrow();

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
    private List<VirtualRegister> concretizeMemoryLocation(MemoryLocation loc, List<Instruction> newList) {
        List<VirtualRegister> mappings = new ArrayList<>();

        if (loc.getBaseRegister().isPresent() && loc.getBaseRegister().get() instanceof VirtualRegister virtReg) {
            var hardwareBaseReg = this.concretizeRegister(virtReg, newList);
            loc.setBaseRegister(hardwareBaseReg);
            mappings.add(virtReg);
        }

        if (loc.getIndex().isPresent() && loc.getIndex().get() instanceof VirtualRegister virtReg) {
            var hardwareIndexReg = this.concretizeRegister(virtReg, newList);
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

                var implicitLowerDividendReg = this.concretizeRegisterInto(HardwareRegister.EAX, dividendVirtReg, newList);
                var implicitUpperDividendReg = this.freeRegisters.getHardwareRegister(HardwareRegister.EDX).orElseThrow();

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

                this.freeRegisters.freeMapping(dividendVirtReg);
                this.freeRegisters.freeMapping(divisorVirtReg);
                this.freeRegisters.freeHardwareRegister(implicitUpperDividendReg);
            }
            case BinaryInstruction binary -> {
                Set<VirtualRegister> allocatedRegisters = new HashSet<>();

                var targetReg = (VirtualRegister) binary.getTarget();
                var lhsVirtReg = (VirtualRegister) binary.getLhs();
                var lhsReg= this.concretizeRegister(lhsVirtReg, newList);
                allocatedRegisters.add(lhsVirtReg);

                if (binary.getRhs() instanceof MemoryLocation rhs) {
                    var memRegs = this.concretizeMemoryLocation(rhs, newList);
                    allocatedRegisters.addAll(memRegs);
                } else if (binary.getRhs() instanceof VirtualRegister rhs) {
                    var rhsHardwareReg = this.concretizeRegister(rhs, newList);
                    binary.setRhs(rhsHardwareReg);
                    allocatedRegisters.add(rhs);
                }

                binary.setTarget(lhsReg);
                binary.setLhs(lhsReg);

                newList.add(binary);

                this.saveVirtualRegister(targetReg, lhsReg, newList);

                allocatedRegisters.forEach(this.freeRegisters::freeMapping);
            }
            case ReturnInstruction ret -> {
                // Load return value into RAX/EAX
                if (ret.getReturnValue().isPresent()) {
                    var retVirtReg = (VirtualRegister)ret.getReturnValue().get();
                    var hardwareReg = this.concretizeRegisterInto(HardwareRegister.EAX.forWidth(retVirtReg.getWidth()), retVirtReg, newList);
                    ret.setReturnValue(hardwareReg);
                    this.freeRegisters.freeMapping(retVirtReg);
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
                var lhsVirtReg = (VirtualRegister) cmp.getLhs();
                var lhsHardwareReg = this.concretizeRegister(lhsVirtReg, newList);
                var rhsVirtReg = (VirtualRegister) cmp.getRhs();
                var rhsHardwareReg = this.concretizeRegister(rhsVirtReg, newList);

                cmp.setLhs(lhsHardwareReg);
                cmp.setRhs(rhsHardwareReg);
                newList.add(cmp);

                this.freeRegisters.freeMapping(lhsVirtReg);
                this.freeRegisters.freeMapping(rhsVirtReg);
            }
            case BranchInstruction branch -> newList.add(branch);
            case AllocCallInstruction allocCall -> {
                var objectSizeVirtReg = (VirtualRegister) allocCall.getObjectSize();
                var objectSizeReg = this.concretizeRegisterInto(HardwareRegister.EDI, objectSizeVirtReg, newList);
                var numElementsVirtReg = (VirtualRegister) allocCall.getNumElements();
                var numElementsReg = this.concretizeRegisterInto(HardwareRegister.ESI, numElementsVirtReg, newList);

                var virtRegTarget = (VirtualRegister) allocCall.getTarget();

                allocCall.setNumElements(numElementsReg);
                allocCall.setObjectSize(objectSizeReg);

                newList.add(allocCall);

                // TODO: make sure RAX is free to be used.
                allocCall.setTarget(HardwareRegister.RAX);

                this.saveVirtualRegister(virtRegTarget, HardwareRegister.RAX, newList);

                this.freeRegisters.freeMapping(objectSizeVirtReg);
                this.freeRegisters.freeMapping(numElementsVirtReg);
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
                            this.freeRegisters.freeMapping(argVirtReg);
                        }

                        newList.add(methodCall);

                        if (!(intrinsic.getReturnTy() instanceof VoidTy)) {
                            // TODO: make sure RAX is free to be used.
                            var targetVirtReg = (VirtualRegister)methodCall.getTarget();
                            // The only intrinsic function that has a return value is System.in.read which returns a 32bit int.
                            this.saveVirtualRegister(targetVirtReg, HardwareRegister.EAX, newList);
                            methodCall.setTarget(HardwareRegister.EAX);
                        }
                    }
                    case DefinedMethod method -> {
                        for (int i = methodCall.getArguments().size() - 1; i >= 0; i--) {
                            var virtReg = (VirtualRegister) methodCall.getArguments().get(i);
                            var hardwareReg = this.concretizeRegister(virtReg, newList);

                            newList.add(new PushInstruction(hardwareReg.forWidth(Register.Width.BIT64)));

                            this.freeRegisters.freeMapping(virtReg);
                        }
                        var requiredStackSpace = methodCall.getArguments().size() * Register.Width.BIT64.getByteSize();

                        // call function
                        newList.add(methodCall);

                        // save return value;
                        if (!(method.getReturnTy() instanceof VoidTy)) {
                            var targetVirtReg = (VirtualRegister) methodCall.getTarget();
                            // TODO: make sure RAX is free to be used.
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
                Set<VirtualRegister> allocatedRegisters = new HashSet<>();

                if (mov.getSource() instanceof VirtualRegister source) {
                    var sourceHardwareReg = this.concretizeRegister(source, newList);
                    allocatedRegisters.add(source);
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
                        var targetHardwareReg = this.freeRegisters.getOrCreateMapping(virtualReg).orElseThrow();
                        mov.setDestination(targetHardwareReg);
                        newList.add(mov);
                        allocatedRegisters.add(virtualReg);
                        this.saveVirtualRegister(virtualReg, targetHardwareReg, newList);
                    }
                    default -> throw new AssertionError("Unexpected mov destination");
                }

                allocatedRegisters.forEach(this.freeRegisters::freeMapping);
            }
            case MovSignExtendInstruction movSX -> {
                if (movSX.getInput() instanceof VirtualRegister virtRegInput) {
                    var inputHardwareReg = this.concretizeRegister(virtRegInput, newList);
                    movSX.setInput(inputHardwareReg);
                    this.freeRegisters.freeMapping(virtRegInput);
                }

                newList.add(movSX);

                if (movSX.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.freeRegisters.getOrCreateMapping(virRegTarget).orElseThrow();
                    this.saveVirtualRegister(virRegTarget, targetHardware, newList);
                    movSX.setTarget(targetHardware);
                    this.freeRegisters.freeMapping(virRegTarget);
                }
            }
            case LoadEffectiveAddressInstruction lea -> {
                var freeList = this.concretizeMemoryLocation(lea.getLoc(), newList);

                newList.add(lea);

                if (lea.getTarget() instanceof VirtualRegister virRegTarget) {
                    var targetHardware = this.freeRegisters.getOrCreateMapping(virRegTarget).orElseThrow();
                    this.saveVirtualRegister(virRegTarget, targetHardware, newList);
                    lea.setTarget(targetHardware);
                    this.freeRegisters.freeMapping(virRegTarget);
                }
                freeList.forEach(this.freeRegisters::freeMapping);
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
