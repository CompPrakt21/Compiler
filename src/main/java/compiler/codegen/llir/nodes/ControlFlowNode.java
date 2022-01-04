package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.ArrayList;
import java.util.List;

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
