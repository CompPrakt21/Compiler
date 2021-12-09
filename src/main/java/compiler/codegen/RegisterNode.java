package compiler.codegen;

public sealed abstract class RegisterNode extends LlirNode permits AddInstruction, MovImmediateInstruction, InputNode {
    protected Register targetRegister;

    public RegisterNode(BasicBlock bb, Register target) {
        super(bb);
        this.targetRegister = target;
    }

    public Register getTargetRegister() {
        return this.targetRegister;
    }
}
