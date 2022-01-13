package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<Register> getReadRegisters() {
        return List.of(this.lhs, this.rhs);
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
