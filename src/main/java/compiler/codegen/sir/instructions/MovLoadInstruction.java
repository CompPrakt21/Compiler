package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MovLoadInstruction extends RegisterInstruction {
    private Register address;

    public Register getAddress() {
        return address;
    }

    public void setAddress(Register address) {
        this.address = address;
    }

    public MovLoadInstruction(Register target, Register address) {
        super(target);
        this.address = address;
    }

    @Override
    public String getMnemonic() {
        return "mov-load";
    }
}
