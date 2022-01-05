package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class SubInstruction extends BinaryInstruction {
    public SubInstruction(Register target, Register lhs, Register rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "sub";
    }
}
