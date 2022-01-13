package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;

public final class PopInstruction extends Instruction {
    private Register register;

    public PopInstruction(Register register) {
        this.register = register;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }

    @Override
    public String getMnemonic() {
        return "pop";
    }

    @Override
    public List<Register> getReadRegisters() {
        return List.of();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.of(this.register);
    }
}
