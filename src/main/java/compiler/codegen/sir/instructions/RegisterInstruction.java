package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.Optional;

public abstract sealed class RegisterInstruction extends Instruction permits BinaryInstruction, CallInstruction, ConvertDoubleToQuadInstruction, DivInstruction, LoadEffectiveAddressInstruction, MovSignExtendInstruction {
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

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.of(this.target);
    }
}
