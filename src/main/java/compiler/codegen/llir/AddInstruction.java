package compiler.codegen.llir;

public final class AddInstruction extends BinaryInstruction {
    public AddInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "add";
    }
}
