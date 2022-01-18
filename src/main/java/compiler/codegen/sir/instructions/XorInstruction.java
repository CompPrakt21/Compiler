package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

public final class XorInstruction extends BinaryInstruction {
    public XorInstruction(Register target, Register lhs, Operand rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "xor";
    }
}
