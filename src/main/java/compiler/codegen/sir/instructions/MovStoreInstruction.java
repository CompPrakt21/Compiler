package compiler.codegen.sir.instructions;

import compiler.codegen.Register;
import compiler.codegen.MemoryLocation;

public final class MovStoreInstruction extends Instruction {
    private MemoryLocation address;
    private Register value;

    public MovStoreInstruction(MemoryLocation address, Register value) {
        this.address = address;
        this.value = value;
    }

    public MemoryLocation getAddress() {
        return address;
    }

    public void setAddress(MemoryLocation address) {
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
