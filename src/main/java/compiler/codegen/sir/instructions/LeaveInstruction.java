package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;

public final class LeaveInstruction extends Instruction {
    @Override
    public String getMnemonic() {
        return "leave";
    }

    @Override
    public List<Register> getReadRegisters() {
        return List.of();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
