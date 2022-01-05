package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class MovRegInstruction extends RegisterInstruction {
    private Register source;

    public Register getSource() {
        return source;
    }

    public void setSource(Register source) {
        this.source = source;
    }

    public MovRegInstruction(Register target, Register source) {
        super(target);
        this.source = source;
    }

    @Override
    public String getMnemonic() {
        return "mov-reg";
    }
}
