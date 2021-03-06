package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

import java.util.List;
import java.util.stream.Stream;

public abstract sealed class BinaryInstruction extends RegisterInstruction permits AddInstruction, SubInstruction, MulInstruction, XorInstruction, AndInstruction {
    protected Register lhs;
    protected Operand rhs;

    public BinaryInstruction(Register target, Register lhs, Operand rhs) {
        super(target);
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public Register getLhs() {
        return lhs;
    }

    public void setLhs(Register lhs) {
        this.lhs = lhs;
    }

    public Operand getRhs() {
        return rhs;
    }

    public void setRhs(Operand rhs) {
        this.rhs = rhs;
    }

    @Override
    public List<Register> getReadRegisters() {
        return Stream.concat(Stream.of(this.lhs), rhs.getRegisters().stream()).toList();
    }
}
