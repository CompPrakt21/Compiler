package compiler.codegen.sir.instructions;

public abstract sealed class ControlFlowInstruction extends Instruction permits ReturnInstruction, JumpInstruction, BranchInstruction {
}
