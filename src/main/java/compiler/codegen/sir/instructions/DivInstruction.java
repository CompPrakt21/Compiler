package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class DivInstruction extends RegisterInstruction {
    private DivType type;

    private Register dividend;
    private Register divisor;

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

    public DivInstruction(Register target, Register dividend, Register divisor, DivType type) {
        super(target);
        this.type = type;
        this.dividend = dividend;
        this.divisor = divisor;
    }

    public Register getDividend() {
        return dividend;
    }

    public void setDividend(Register dividend) {
        this.dividend = dividend;
    }

    public Register getDivisor() {
        return divisor;
    }

    public void setDivisor(Register divisor) {
        this.divisor = divisor;
    }

    public DivType getType() {
        return type;
    }
}
