package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class XorFromMemInstruction extends BinaryFromMemInstruction {
    public XorFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        super(bb, lhs, rhs, sideEffect);
    }

    @Override
    public String getMnemonic() {
        return "xor";
    }
}
