package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class CmpInstruction extends Instruction {
    private Register lhs;
    private Operand rhs;

    public CmpInstruction(Register lhs, Operand rhs) {
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
    public String getMnemonic() {
        return "cmp";
    }

    @Override
    public List<Register> getReadRegisters() {
        return Stream.concat(Stream.of(lhs), rhs.getRegisters().stream()).toList();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
