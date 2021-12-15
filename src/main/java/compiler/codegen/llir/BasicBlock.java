package compiler.codegen.llir;

import compiler.semantic.resolution.MethodDefinition;
import firm.nodes.Call;
import firm.nodes.Div;

import java.util.*;
import java.util.stream.Collectors;

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
        var input = new InputNode(this, register);
        this.inputNodes.add(input);
        return input;
    }

    public List<LlirNode> getOutputNodes() {
        return this.outputNodes;
    }

    public void addOutput(LlirNode out) {
        this.outputNodes.add(out);
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

            queue.addAll(node.getPreds().filter(p -> !result.contains(p)).collect(Collectors.toList()));
            result.addAll(node.getPreds().collect(Collectors.toList()));
        }

        return result;
    }

    /**
     * Here are all construction methods for all Llir nodes.
     */

    public MovImmediateInstruction newMovImmediate(int constant) {
        return new MovImmediateInstruction(this, constant);
    }

    public MovImmediateInstruction newMovImmediateInto(int constant, Register target) {
        return new MovImmediateInstruction(this, constant, target);
    }

    public MovRegisterInstruction newMovRegisterInto(Register target, RegisterNode source) {
        return new MovRegisterInstruction(this, target, source);
    }

    public AddInstruction newAdd(RegisterNode lhs, RegisterNode rhs) {
        return new AddInstruction(this, lhs, rhs);
    }

    public SubInstruction newSub(RegisterNode lhs, RegisterNode rhs) {
        return new SubInstruction(this, lhs, rhs);
    }

    public MulInstruction newMul(RegisterNode lhs, RegisterNode rhs) {
        return new MulInstruction(this, lhs, rhs);
    }

    public XorInstruction newXor(RegisterNode lhs, RegisterNode rhs) {
        return new XorInstruction(this, lhs, rhs);
    }

    public DivInstruction newDiv(RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect) {
        return new DivInstruction(this, dividend, divisor, sideEffect);
    }

    public ModInstruction newMod(RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect) {
        return new ModInstruction(this, dividend, divisor, sideEffect);
    }

    public MovStoreInstruction newMovStore(RegisterNode addr, RegisterNode value, SideEffect sideEffect) {
        return new MovStoreInstruction(this, sideEffect, addr, value);
    }

    public MovLoadInstruction newMovLoad(RegisterNode addr, SideEffect sideEffect) {
        return new MovLoadInstruction(this, sideEffect, addr);
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

    public CmpInstruction newCmp(RegisterNode lhs, RegisterNode rhs) {
        return new CmpInstruction(this, lhs, rhs);
    }

    public BranchInstruction newBranch(BranchInstruction.Predicate predicate, CmpInstruction cmp, BasicBlock trueBlock, BasicBlock falseBlock) {
        return new BranchInstruction(this, predicate, cmp, trueBlock, falseBlock);
    }

    public CallInstruction newCall(MethodDefinition calledMethod, SideEffect sideEffect, List<RegisterNode> arguments) {
        return new CallInstruction(this, calledMethod, sideEffect, arguments);
    }
}
