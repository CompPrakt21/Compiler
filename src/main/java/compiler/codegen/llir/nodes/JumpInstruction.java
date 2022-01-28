package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class JumpInstruction extends ControlFlowNode {

    public JumpInstruction(BasicBlock bb, BasicBlock target) {
        super(bb);
        this.basicBlock = bb;
        this.targets.add(0, target);
    }

    public BasicBlock getTarget() {
        return this.targets.get(0);
    }

    public void setTarget(BasicBlock target) {
        this.targets.set(0, target);
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
