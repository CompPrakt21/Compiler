package compiler.codegen.sir.instructions;

public abstract sealed class Instruction permits CmpInstruction, ControlFlowInstruction, LeaveInstruction, MovStoreInstruction, PopInstruction, PushInstruction, RegisterInstruction {
    public abstract String getMnemonic();
}
