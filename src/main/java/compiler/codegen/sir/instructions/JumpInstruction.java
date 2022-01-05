package compiler.codegen.sir.instructions;

import compiler.codegen.sir.BasicBlock;

public final class JumpInstruction extends ControlFlowInstruction {
    private BasicBlock target;

    public JumpInstruction(BasicBlock target) {
        this.target = target;
    }

    public void setTarget(BasicBlock target) {
        this.target = target;
    }

    public BasicBlock getTarget() {
        return target;
    }

    @Override
    public String getMnemonic() {
        return "jmp";
    }
}
