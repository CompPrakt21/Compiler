package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class AndFromMemInstruction extends BinaryFromMemInstruction {
    public AndFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        super(bb, lhs, rhs, sideEffect);
    }

    @Override
    public String getMnemonic() {
        return "and";
    }
}
