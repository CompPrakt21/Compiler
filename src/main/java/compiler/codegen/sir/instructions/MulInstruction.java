package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MulInstruction extends BinaryInstruction {
    public MulInstruction(Register target, Register lhs, Register rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "mul";
    }
}
