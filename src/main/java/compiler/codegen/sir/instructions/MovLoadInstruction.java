package compiler.codegen.sir.instructions;

import compiler.codegen.Register;
import compiler.codegen.MemoryLocation;

public final class MovLoadInstruction extends RegisterInstruction {
    private MemoryLocation address;

    public MemoryLocation getAddress() {
        return address;
    }

    public void setAddress(MemoryLocation address) {
        this.address = address;
    }

    public MovLoadInstruction(Register target, MemoryLocation address) {
        super(target);
        this.address = address;
    }

    @Override
    public String getMnemonic() {
        return "mov-load";
    }
}
