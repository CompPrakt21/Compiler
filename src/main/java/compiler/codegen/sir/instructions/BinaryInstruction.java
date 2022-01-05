package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public abstract sealed class BinaryInstruction extends RegisterInstruction permits AddInstruction, DivInstruction, SubInstruction, MulInstruction, XorInstruction {
    protected Register lhs;
    protected Register rhs;

    public BinaryInstruction(Register target, Register lhs, Register rhs) {
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

    public Register getRhs() {
        return rhs;
    }

    public void setRhs(Register rhs) {
        this.rhs = rhs;
    }
}
