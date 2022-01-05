package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MovStoreInstruction extends Instruction {
    private Register address;
    private Register value;

    public MovStoreInstruction(Register address, Register value) {
        this.address = address;
        this.value = value;
    }

    public Register getAddress() {
        return address;
    }

    public void setAddress(Register address) {
        this.address = address;
    }

    public Register getValue() {
        return value;
    }

    public void setValue(Register value) {
        this.value = value;
    }

    @Override
    public String getMnemonic() {
        return "mov-store";
    }
}
