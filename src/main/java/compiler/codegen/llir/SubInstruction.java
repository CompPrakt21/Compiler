package compiler.codegen.llir;

public final class SubInstruction extends BinaryInstruction {
    public SubInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "sub";
    }
}
