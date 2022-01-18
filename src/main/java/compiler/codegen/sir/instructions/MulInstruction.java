package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

public final class MulInstruction extends BinaryInstruction {
    public MulInstruction(Register target, Register lhs, Operand rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "mul";
    }
}
