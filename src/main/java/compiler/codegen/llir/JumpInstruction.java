package compiler.codegen.llir;

import java.util.stream.Stream;

public final class JumpInstruction extends ControlFlowNode {

    public JumpInstruction(BasicBlock bb, BasicBlock target, SideEffect sideEffect) {
        super(bb, sideEffect);
        this.basicBlock = bb;
        this.targets.add(0, target);
    }

    public BasicBlock getTarget() {
        return this.targets.get(0);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return super.getPreds();
    }

    @Override
    public int getPredSize() {
        return super.getPredSize();
    }

    @Override
    public String getMnemonic() {
        return "jmp";
    }
}
