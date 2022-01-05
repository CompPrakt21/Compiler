package compiler.codegen.sir.instructions;

public abstract sealed class Instruction permits CmpInstruction, ControlFlowInstruction, MovStoreInstruction, RegisterInstruction {
    public abstract String getMnemonic();
}
