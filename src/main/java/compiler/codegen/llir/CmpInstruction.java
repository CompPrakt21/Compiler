package compiler.codegen.llir;

import java.util.stream.Stream;

public final class CmpInstruction extends LlirNode {

    private RegisterNode lhs;
    private RegisterNode rhs;

    public CmpInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb);

        this.lhs = lhs;
        this.rhs = rhs;
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
        return "cmp";
    }

}
