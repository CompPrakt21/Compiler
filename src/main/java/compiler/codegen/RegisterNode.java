package compiler.codegen;

public sealed abstract class RegisterNode extends LlirNode permits AddInstruction, MovImmediateInstruction, InputNode {
    protected Register targetRegister;

    public RegisterNode() {
        super();
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
