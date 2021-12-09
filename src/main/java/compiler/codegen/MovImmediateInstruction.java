package compiler.codegen;

import java.util.stream.Stream;

public final class MovImmediateInstruction extends RegisterNode {

    private int immediateValue;

    public MovImmediateInstruction(BasicBlock bb, Register target, int immediateValue) {
        super(bb, target);
        this.immediateValue = immediateValue;
    }

    public int getImmediateValue() {
        return immediateValue;
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
        return "mov";
    }
}
