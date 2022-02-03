package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public abstract sealed class BinaryInstruction extends RegisterNode permits AddInstruction, MulInstruction, SubInstruction, XorInstruction, AndInstruction {

    protected RegisterNode lhs;
    protected SimpleOperand rhs;

    public BinaryInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb);

        this.lhs = lhs;
        this.rhs = rhs;

        assert !(rhs instanceof RegisterNode regRhs) || regRhs.getTargetRegister().getWidth() == lhs.getTargetRegister().getWidth();

        this.initTargetRegister(lhs.getTargetRegister());
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
        return super.getPredSize() + 2;
    }
}
