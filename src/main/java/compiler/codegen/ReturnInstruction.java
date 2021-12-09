package compiler.codegen;

import java.util.stream.Stream;

public final class ReturnInstruction extends ControlFlowNode {

    private RegisterNode returnValue;

    public ReturnInstruction(RegisterNode returnValue) {
        this.returnValue = returnValue;
        this.inferBasicBlock();
    }

    public RegisterNode getReturnValue() {
        return this.returnValue;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(this.returnValue);
    }

    @Override
    public int getPredSize() {
        return 1;
    }

    @Override
    public String getMnemonic() {
        return "ret";
    }
}
