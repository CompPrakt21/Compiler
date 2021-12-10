package compiler.codegen.llir;

import java.util.stream.Stream;

public final class MovImmediateInstruction extends RegisterNode {

    private int immediateValue;

    public MovImmediateInstruction(BasicBlock bb, int immediateValue) {
        super(bb);
        this.immediateValue = immediateValue;
        this.initTargetRegister();
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
