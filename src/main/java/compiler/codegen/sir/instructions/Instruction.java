package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

import java.util.List;
import java.util.Optional;

public abstract sealed class Instruction permits CmpInstruction, ControlFlowInstruction, LeaveInstruction, MovInstruction, PopInstruction, PushInstruction, RegisterInstruction {
    public abstract String getMnemonic();

    /**
     * @return Registers which are read by the instruction.
     */
    public abstract List<Register> getReadRegisters();

    /**
     * @return The register that is written to by the instruction. (If there is one)
     */
    public abstract Optional<Register> getWrittenRegister();
}
