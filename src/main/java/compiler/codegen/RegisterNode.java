package compiler.codegen;

public non-sealed abstract class RegisterNode extends LlirNode {
    protected Register targetRegister;

    public RegisterNode(Register target) {
        this.targetRegister = target;
    }

    public Register getTargetRegister() {
        return this.targetRegister;
    }
}
