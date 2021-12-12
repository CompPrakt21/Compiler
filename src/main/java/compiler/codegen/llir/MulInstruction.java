package compiler.codegen.llir;

public final class MulInstruction extends BinaryInstruction {
    public MulInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "imul";
    }
}
