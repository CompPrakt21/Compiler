package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.List;

public sealed abstract class RegisterNode extends LlirNode implements SimpleOperand permits BinaryFromMemInstruction, BinaryInstruction, CallInstruction, DivInstruction, InputNode, LoadEffectiveAddressInstruction, MovImmediateInstruction, MovLoadInstruction, MovRegisterInstruction, MovSignExtendInstruction, ShiftInstruction {
    protected Register targetRegister;

    public RegisterNode(BasicBlock bb) {
        super(bb);
    }

    protected void initTargetRegister(Register.Width width) {
        this.targetRegister = this.getBasicBlock().getGraph().getVirtualRegGenerator().nextRegister(width);
    }

    protected void initTargetRegister(Register... inputs) {
        Register.Width width = null;
        for (var input : inputs) {
            if (width == null) {
                width = input.getWidth();
            } else if (width != input.getWidth()) {
                throw new IllegalArgumentException("Unclear register width.");
            }
        }

        this.targetRegister = this.getBasicBlock().getGraph().getVirtualRegGenerator().nextRegister(width);
    }

    public Register getTargetRegister() {
        return this.targetRegister;
    }

    public void setTargetRegister(Register targetRegister) {
        this.targetRegister = targetRegister;
    }

    @Override
    public String formatIntelSyntax() {
        return this.targetRegister.formatIntelSyntax();
    }

    @Override
    public List<RegisterNode> getRegisters() {
        return List.of(this);
    }
}
