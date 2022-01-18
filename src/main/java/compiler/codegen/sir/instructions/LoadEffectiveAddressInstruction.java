package compiler.codegen.sir.instructions;

import compiler.codegen.MemoryLocation;
import compiler.codegen.Register;

import java.util.List;

public final class LoadEffectiveAddressInstruction extends RegisterInstruction {

    private MemoryLocation loc;

    public LoadEffectiveAddressInstruction(Register target, MemoryLocation loc) {
        super(target);
        this.loc = loc;
    }

    public MemoryLocation getLoc() {
        return loc;
    }

    @Override
    public String getMnemonic() {
        return "lea";
    }

    @Override
    public List<Register> getReadRegisters() {
        return this.loc.getRegisters();
    }
}
