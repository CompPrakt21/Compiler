package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class CmpInstruction extends Instruction {
    private Register lhs;
    private Register rhs;

    public CmpInstruction(Register lhs, Register rhs) {
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

    @Override
    public String getMnemonic() {
        return "cmp";
    }
}
