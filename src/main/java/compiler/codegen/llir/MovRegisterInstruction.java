package compiler.codegen.llir;

import java.util.stream.Stream;

public final class MovRegisterInstruction extends RegisterNode {

    private RegisterNode source;

    public MovRegisterInstruction(BasicBlock bb, Register target, RegisterNode source) {
        super(bb);
        this.source = source;
        this.targetRegister = target;
    }

    public RegisterNode getSourceRegister() {
        return this.source;
    }

    public void setSource(RegisterNode source) {
        this.source = source;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(source);
    }

    @Override
    public int getPredSize() {
        return 1;
    }

    @Override
    public String getMnemonic() {
        return "mov";
    }
}
