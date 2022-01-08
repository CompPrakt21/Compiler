package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public abstract sealed class RegisterInstruction extends Instruction permits BinaryInstruction, CallInstruction, ConvertDoubleToQuadInstruction, DivInstruction, MovImmediateInstruction, MovLoadInstruction, MovRegInstruction, MovSignExtendInstruction {
    protected Register target;

    public RegisterInstruction(Register target) {
        this.target = target;
    }

    public Register getTarget() {
        return target;
    }

    public void setTarget(Register target) {
        this.target = target;
    }
}
