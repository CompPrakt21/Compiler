package compiler.codegen.sir.instructions;

import compiler.codegen.sir.BasicBlock;

import java.util.List;

public abstract sealed class ControlFlowInstruction extends Instruction permits ReturnInstruction, JumpInstruction, BranchInstruction {
    public abstract List<BasicBlock> getTargets();
}
