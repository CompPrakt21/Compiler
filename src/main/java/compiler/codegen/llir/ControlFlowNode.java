package compiler.codegen.llir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public sealed abstract class ControlFlowNode extends LlirNode permits JumpInstruction, ReturnInstruction, BranchInstruction {
    protected List<BasicBlock> targets;

    public ControlFlowNode(BasicBlock bb) {
        super(bb);
        this.targets = new ArrayList<>();
    }

    public List<BasicBlock> getTargets() {
        return this.targets;
    }
}
