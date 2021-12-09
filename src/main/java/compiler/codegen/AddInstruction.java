package compiler.codegen;

import java.util.stream.Stream;

public final class AddInstruction extends RegisterNode {

    private RegisterNode lhs;
    private RegisterNode rhs;

    public AddInstruction(RegisterNode lhs, RegisterNode rhs) {
        super();

        this.lhs = lhs;
        this.rhs = rhs;

        this.inferBasicBlock();
        this.initTargetRegister();
    }

    public RegisterNode getLhs() {
        return this.lhs;
    }

    public RegisterNode getRhs() {
        return this.rhs;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(lhs, rhs);
    }

    @Override
    public int getPredSize() {
        return 2;
    }

    @Override
    public String getMnemonic() {
        return "add";
    }
}
