package compiler.codegen.sir.instructions;

import compiler.codegen.Operand;
import compiler.codegen.Register;

import java.util.List;

public final class ShiftLeftInstruction extends ShiftInstruction {
    public ShiftLeftInstruction(Register target, Register lhs, Operand rhs) {
        super(target, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "shl";
    }
}
