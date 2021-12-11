package compiler.codegen.llir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class BasicBlock {
    private String label;

    private List<RegisterNode> outputNodes;

    private List<InputNode> inputNodes;

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

    public InputNode addInput(Register register) {
        var input = new InputNode(this, register);
        this.inputNodes.add(input);
        return input;
    }

    public List<RegisterNode> getOutputNodes() {
        return this.outputNodes;
    }

    public void addOutput(RegisterNode out) {
        this.outputNodes.add(out);
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
}
