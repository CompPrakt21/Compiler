package compiler.codegen.sir.instructions;

import compiler.codegen.Register;
import compiler.codegen.sir.BasicBlock;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ReturnInstruction extends ControlFlowInstruction {
    private Optional<Register> returnValue;

    public ReturnInstruction(Optional<Register> returnValue) {
        this.returnValue = returnValue;
    }

    public void setReturnValue(Register returnValue) {
        this.returnValue = Optional.of(returnValue);
    }

    public Optional<Register> getReturnValue() {
        return returnValue;
    }

    @Override
    public String getMnemonic() {
        return "ret";
    }

    @Override
    public List<Register> getReadRegisters() {
        return returnValue.stream().toList();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }

    @Override
    public List<BasicBlock> getTargets() {
        return List.of();
    }
}
