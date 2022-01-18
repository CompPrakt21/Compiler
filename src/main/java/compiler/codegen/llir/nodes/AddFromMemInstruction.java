package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class AddFromMemInstruction extends BinaryFromMemInstruction {

    public AddFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        super(bb, lhs, rhs, sideEffect);
    }

    @Override
    public String getMnemonic() {
        return "add";
    }
}
