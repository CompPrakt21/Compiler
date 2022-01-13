package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;

public final class PushInstruction extends Instruction {
    private Register register;

    public PushInstruction(Register register) {
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
        return "push";
    }

    @Override
    public List<Register> getReadRegisters() {
        return List.of(this.register);
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
