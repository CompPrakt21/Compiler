package compiler.codegen.llir;

public sealed abstract class RegisterNode extends LlirNode permits BinaryInstruction, DivInstruction, InputNode, ModInstruction, MovImmediateInstruction, MovLoadInstruction, MovRegisterInstruction {
    protected Register targetRegister;

    public RegisterNode(BasicBlock bb) {
        super(bb);
    }

    protected void initTargetRegister() {
        this.targetRegister = this.getBasicBlock().getGraph().getVirtualRegGenerator().nextRegister();
    }

    public Register getTargetRegister() {
        return this.targetRegister;
    }

    public void setTargetRegister(Register targetRegister) {
        this.targetRegister = targetRegister;
    }
}
