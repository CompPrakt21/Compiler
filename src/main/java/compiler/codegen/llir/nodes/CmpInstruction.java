package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class CmpInstruction extends LlirNode implements CmpLikeInstruction {

    private RegisterNode lhs;
    private SimpleOperand rhs;

    public CmpInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb);

        this.lhs = lhs;
        this.rhs = rhs;
    }

    public RegisterNode getLhs() {
        return this.lhs;
    }

    public SimpleOperand getRhs() {
        return this.rhs;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(lhs), rhs.getRegisters().stream()));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 1 + rhs.getRegisters().size();
    }

    @Override
    public String getMnemonic() {
        return "cmp";
    }
}
