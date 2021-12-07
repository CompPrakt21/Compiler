package compiler.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public non-sealed abstract class ControlFlowNode extends LlirNode {
    protected List<BasicBlock> targets;

    // A ControlFlowNode is always the last instruction of a BasicBlock to be
    // executed.
    // Therefore it is given every other instruction in its BasicBlock to
    // ensure it is scheduled last.
    protected List<LlirNode> preds;

    public ControlFlowNode(List<LlirNode> preds) {
        this.preds = preds;
        this.targets = new ArrayList<>();
    }

    public List<BasicBlock> getTargets() {
        return this.targets;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return preds.stream();
    }

    @Override
    public int getPredSize() {
        return preds.size();
    }
}
