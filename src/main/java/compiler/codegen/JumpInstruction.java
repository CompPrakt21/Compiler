package compiler.codegen;

import java.util.List;
import java.util.stream.Stream;

public final class JumpInstruction extends ControlFlowNode {

    public JumpInstruction(BasicBlock bb, BasicBlock target) {
        this.basicBlock = bb;
        this.targets.add(0, target);
    }

    public BasicBlock getTarget() {
        return this.targets.get(0);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.empty();
    }

    @Override
    public int getPredSize() {
        return 0;
    }

    @Override
    public String getMnemonic() {
        return "jmp";
    }
}
