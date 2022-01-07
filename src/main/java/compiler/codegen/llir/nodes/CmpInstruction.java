package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

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
        return Stream.concat(super.getPreds(), Stream.of(lhs, rhs));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2;
    }

    @Override
    public String getMnemonic() {
        return "cmp";
    }

}
