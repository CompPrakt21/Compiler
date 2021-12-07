package compiler.codegen;

import java.util.stream.Stream;

public class MovImmediateInstruction extends RegisterNode {

    private int immediateValue;

    public MovImmediateInstruction(Register target, int immediateValue) {
        super(target);
        this.immediateValue = immediateValue;
    }

    public int getImmediateValue() {
        return immediateValue;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return null;
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
