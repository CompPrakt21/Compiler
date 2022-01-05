package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

public sealed abstract class RegisterNode extends LlirNode permits BinaryInstruction, CallInstruction, DivInstruction, InputNode, MovImmediateInstruction, MovLoadInstruction, MovRegisterInstruction, MovSignExtendInstruction {
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
}
