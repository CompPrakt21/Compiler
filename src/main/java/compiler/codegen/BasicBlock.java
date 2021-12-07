package compiler.codegen;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BasicBlock {
    private String label;

    private List<RegisterNode> outputNodes;

    private List<InputNode> inputNodes;

    private ControlFlowNode endNode;

    private boolean finishedConstruction;

    public BasicBlock(String label) {
        this.label = label;
        this.inputNodes = new ArrayList<>();
        this.outputNodes = new ArrayList<>();
        this.finishedConstruction = false;
        this.endNode = null;
    }

    public void finish(ControlFlowNode endNode) {
        this.finishedConstruction = true;
        this.endNode = endNode;
    }

    public boolean isFinished() {
        return this.finishedConstruction;
    }

    public String getLabel() {
        return this.label;
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

    public void addInput(Register register) {
        this.inputNodes.add(new InputNode(register));
    }

    public List<RegisterNode> getOutputNodes() {
        return this.outputNodes;
    }

    public void addOutput(RegisterNode out) {
        this.outputNodes.add(out);
    }
}
