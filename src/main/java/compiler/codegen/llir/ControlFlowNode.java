package compiler.codegen.llir;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public sealed abstract class ControlFlowNode extends LlirNode permits JumpInstruction, ReturnInstruction {
    protected List<BasicBlock> targets;

    protected SideEffect sideEffect;

    public ControlFlowNode(BasicBlock bb, SideEffect sideEffect) {
        super(bb);
        this.targets = new ArrayList<>();
        this.sideEffect = sideEffect;
    }

    public List<BasicBlock> getTargets() {
        return this.targets;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(this.sideEffect.asLlirNode());
    }

    @Override
    public int getPredSize() {
        return 1;
    }
}
