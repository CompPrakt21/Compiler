package compiler.codegen;

import java.util.ArrayList;
import java.util.List;

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
}
