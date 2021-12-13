package compiler.codegen.llir;

public final class XorInstruction extends BinaryInstruction {

    public XorInstruction(BasicBlock bb, RegisterNode lhs, RegisterNode rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "xor";
    }
}
