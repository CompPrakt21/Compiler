package compiler.codegen.llir;

import compiler.codegen.Predicate;
import compiler.codegen.Register;
import compiler.codegen.llir.nodes.*;
import compiler.semantic.resolution.MethodDefinition;

import java.util.*;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BasicBlock {
    private String label;

    private List<LlirNode> outputNodes;

    private List<InputNode> inputNodes;

    private MemoryInputNode memoryInput;

    private ControlFlowNode endNode;

    private boolean finishedConstruction;

    private final LlirGraph graph;

    public BasicBlock(LlirGraph graph, String label) {
        this.label = label;
        this.inputNodes = new ArrayList<>();
        this.outputNodes = new ArrayList<>();
        this.finishedConstruction = false;
        this.endNode = null;
        this.graph = graph;
        this.memoryInput = new MemoryInputNode(this);
    }

    public void finish(ControlFlowNode endNode) {
        if (this.finishedConstruction) {
            throw new IllegalStateException("Construction of BasicBlock is already finished.");
        }

        this.finishedConstruction = true;
        this.endNode = endNode;
    }

    public boolean isFinished() {
        return this.finishedConstruction;
    }

    public String getLabel() {
        return this.label;
    }

    public LlirGraph getGraph() {
        return this.graph;
    }

    public ControlFlowNode getEndNode() {
        if (this.finishedConstruction) {
            return this.endNode;
        } else {
            throw new IllegalCallerException("Construction of basic block is unfinished.");
        }
    }

    public List<InputNode> getInputNodes() {
        return this.inputNodes;
    }

    public MemoryInputNode getMemoryInput() {
        return this.memoryInput;
    }

    public InputNode addInput(Register register) {
        for (var i : this.inputNodes) {
            if (i.getTargetRegister().equals(register)) {
                return i;
            }
        }

        var input = new InputNode(this, register);
        this.inputNodes.add(input);
        return input;
    }

    public List<LlirNode> getOutputNodes() {
        return this.outputNodes;
    }

    public void addOutput(LlirNode out) {
        assert out != null;
        if (!this.outputNodes.contains(out)) {
            this.outputNodes.add(out);
        }
    }

    public Collection<LlirNode> getAllNodes() {
        var result = new HashSet<LlirNode>();
        var queue = new ArrayDeque<LlirNode>();

        result.add(this.getEndNode());
        queue.add(this.getEndNode());
        for (var out: this.getOutputNodes()) {
            result.add(out);
            queue.add(out);
        }

        while (!queue.isEmpty()) {
            var node = queue.pop();

            queue.addAll(node.getPreds().filter(p -> !result.contains(p)).toList());
            result.addAll(node.getPreds().toList());
        }

        return result;
    }

    /**
     * Here are all construction methods for all Llir nodes.
     */

    public MovImmediateInstruction newMovImmediate(long constant, Register.Width width) {
        return new MovImmediateInstruction(this, constant, width);
    }

    public MovImmediateInstruction newMovImmediateInto(long constant, Register target, Register.Width width) {
        return new MovImmediateInstruction(this, constant, target, width);
    }

    public MovRegisterInstruction newMovRegisterInto(Register target, RegisterNode source) {
        return new MovRegisterInstruction(this, target, source);
    }

    public MovSignExtendInstruction newMovSignExtend(RegisterNode input) {
        return new MovSignExtendInstruction(this, input);
    }

    public AddInstruction newAdd(RegisterNode lhs, SimpleOperand rhs) {
        return new AddInstruction(this, lhs, rhs);
    }

    public AddFromMemInstruction newAddFromMem(RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        return new AddFromMemInstruction(this, lhs, rhs, sideEffect);
    }

    public SubInstruction newSub(RegisterNode lhs, SimpleOperand rhs) {
        return new SubInstruction(this, lhs, rhs);
    }

    public SubFromMemInstruction newSubFromMem(RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        return new SubFromMemInstruction(this, lhs, rhs, sideEffect);
    }

    public MulInstruction newMul(RegisterNode lhs, SimpleOperand rhs) {
        return new MulInstruction(this, lhs, rhs);
    }

    public MulFromMemInstruction newMulFromMem(RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        return new MulFromMemInstruction(this, lhs, rhs, sideEffect);
    }

    public XorInstruction newXor(RegisterNode lhs, SimpleOperand rhs) {
        return new XorInstruction(this, lhs, rhs);
    }

    public XorFromMemInstruction newXorFromMem(RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        return new XorFromMemInstruction(this, lhs, rhs, sideEffect);
    }

    public DivInstruction newDiv(RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect) {
        return new DivInstruction(this, dividend, divisor, sideEffect, DivInstruction.DivType.Div);
    }

    public DivInstruction newMod(RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect) {
        return new DivInstruction(this, dividend, divisor, sideEffect, DivInstruction.DivType.Mod);
    }

    public MovStoreInstruction newMovStore(MemoryLocation addr, RegisterNode value, SideEffect sideEffect, Register.Width width) {
        return new MovStoreInstruction(this, sideEffect, addr, value, width);
    }

    public MovLoadInstruction newMovLoad(MemoryLocation loc, SideEffect sideEffect, Register.Width outputWidth) {
        return new MovLoadInstruction(this, sideEffect, loc, outputWidth);
    }

    public LoadEffectiveAddressInstruction newLoadEffectiveAddress(Register.Width width, MemoryLocation loc) {
        return new LoadEffectiveAddressInstruction(this, width, loc);
    }

    public InputNode newInput(Register register) {
        return new InputNode(this, register);
    }

    public JumpInstruction newJump(BasicBlock target) {
        return new JumpInstruction(this, target);
    }

    public ReturnInstruction newReturn(Optional<RegisterNode> returnValue) {
        return new ReturnInstruction(this, returnValue);
    }

    public CmpInstruction newCmp(RegisterNode lhs, SimpleOperand rhs, boolean reversedArguments) {
        return new CmpInstruction(this, lhs, rhs, reversedArguments);
    }

    public CmpFromMemInstruction newCmpFromMem(RegisterNode lhs, MemoryLocation rhs, boolean reversedArguments, SideEffect sideEffect) {
        return new CmpFromMemInstruction(this, lhs, rhs, reversedArguments, sideEffect);
    }

    public BranchInstruction newBranch(Predicate predicate, CmpLikeInstruction cmp, BasicBlock trueBlock, BasicBlock falseBlock) {
        return new BranchInstruction(this, predicate, cmp, trueBlock, falseBlock);
    }

    public AllocCallInstruction newAllocCall(SideEffect sideEffect, RegisterNode numElements, RegisterNode elemSize) {
        return new AllocCallInstruction(this, sideEffect, numElements, elemSize);
    }

    public MethodCallInstruction newMethodCall(MethodDefinition calledMethod, SideEffect sideEffect, List<RegisterNode> arguments) {
        return new MethodCallInstruction(this, calledMethod, sideEffect, arguments);
    }
}
