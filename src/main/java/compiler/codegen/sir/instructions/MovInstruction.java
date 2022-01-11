package compiler.codegen.sir.instructions;

import compiler.codegen.Constant;
import compiler.codegen.Register.Width;
import compiler.codegen.Operand;
import compiler.codegen.Register;

public final class MovInstruction extends Instruction {
    private Register.Width width;
    private Operand destination;
    private Operand source;

    public MovInstruction(Register.Width width, Operand destination, Operand source) {
        this.width = width;
        this.destination = destination;
        this.source = source;

        assert this.verify();
    }

    private boolean verify() {
        return (this.destination instanceof Register || this.source instanceof Register)
                && !(this.destination instanceof Constant);
    }

    public Register.Width getWidth() {
        return width;
    }

    public void setWidth(Register.Width width) {
        this.width = width;
    }

    public Operand getDestination() {
        return destination;
    }

    public void setDestination(Operand destination) {
        this.destination = destination;
        assert this.verify();
    }

    public Operand getSource() {
        return source;
    }

    public void setSource(Operand source) {
        this.source = source;
        assert this.verify();
    }

    @Override
    public String getMnemonic() {
        return "mov";
    }
}
