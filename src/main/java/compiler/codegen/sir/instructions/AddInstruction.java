package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class AddInstruction extends BinaryInstruction {
    public AddInstruction(Register target, Register lhs, Register rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "add";
    }
}
