package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class DivInstruction extends BinaryInstruction {
    private DivType type;

    @Override
    public String getMnemonic() {
        return switch (this.type) {
            case Div -> "div";
            case Mod -> "div (mod)";
        };
    }

    public enum DivType {
        Div, Mod
    }

    public DivInstruction(Register target, Register lhs, Register rhs, DivType type) {
        super(target, lhs, rhs);
        this.type = type;
    }

    public DivType getType() {
        return type;
    }
}
