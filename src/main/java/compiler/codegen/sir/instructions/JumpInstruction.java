package compiler.codegen.sir.instructions;

import compiler.codegen.Register;
import compiler.codegen.sir.BasicBlock;

import java.util.List;
import java.util.Optional;

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

    @Override
    public List<Register> getReadRegisters() {
        return List.of();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
