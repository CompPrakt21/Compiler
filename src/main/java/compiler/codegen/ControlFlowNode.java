package compiler.codegen;

import java.util.ArrayList;
import java.util.List;

public sealed abstract class ControlFlowNode extends LlirNode permits JumpInstruction, ReturnInstruction {
    protected List<BasicBlock> targets;

    public ControlFlowNode() {
        this.targets = new ArrayList<>();
    }

    public List<BasicBlock> getTargets() {
        return this.targets;
    }
}
