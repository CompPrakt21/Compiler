package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MovSignExtendInstruction extends RegisterInstruction {
    private Register input;

    public MovSignExtendInstruction(Register target, Register input) {
        super(target);
        this.input = input;
    }

    public Register getInput() {
        return input;
    }

    @Override
    public String getMnemonic() {
        return "mov-sx";
    }
}
