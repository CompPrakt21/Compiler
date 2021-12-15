package compiler.codegen.llir;

import java.util.stream.Stream;

public abstract sealed class BinaryInstruction extends RegisterNode permits AddInstruction, MulInstruction, SubInstruction, XorInstruction {

    protected RegisterNode lhs;
    protected RegisterNode rhs;

    public BinaryInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb);

        this.lhs = lhs;
        this.rhs = rhs;

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
}
