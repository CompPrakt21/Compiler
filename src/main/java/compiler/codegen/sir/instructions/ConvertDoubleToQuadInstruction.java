package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class ConvertDoubleToQuadInstruction extends RegisterInstruction {

    private Register doubleWord;

    public ConvertDoubleToQuadInstruction(Register target, Register doubleWord) {
        super(target);
        assert doubleWord.getWidth() == Register.Width.BIT32;
        this.doubleWord = doubleWord;
    }

    public Register getDoubleWord() {
        return doubleWord;
    }

    public void setDoubleWord(Register doubleWord) {
        this.doubleWord = doubleWord;
    }

    @Override
    public String getMnemonic() {
        return "cdq";
    }
}
