package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;

public final class MovSignExtendInstruction extends RegisterInstruction {
    private Register input;

    public MovSignExtendInstruction(Register target, Register input) {
        super(target);
        this.input = input;
    }

    public Register getInput() {
        return input;
    }

    public void setInput(Register input) {
        this.input = input;
    }

    @Override
    public String getMnemonic() {
        return "mov-sx";
    }

    @Override
    public List<Register> getReadRegisters() {
        return List.of(this.input);
    }
}
