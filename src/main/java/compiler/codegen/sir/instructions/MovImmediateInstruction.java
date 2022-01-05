package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MovImmediateInstruction extends RegisterInstruction {
    private int immediateValue;

    public int getImmediateValue() {
        return immediateValue;
    }

    public MovImmediateInstruction(Register target, int immediateValue) {
        super(target);
        this.immediateValue = immediateValue;
    }

    @Override
    public String getMnemonic() {
        return "mov-imm";
    }
}
