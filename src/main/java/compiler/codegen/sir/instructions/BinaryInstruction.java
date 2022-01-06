package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

public abstract sealed class BinaryInstruction extends RegisterInstruction permits AddInstruction, DivInstruction, SubInstruction, MulInstruction, XorInstruction {
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
}
