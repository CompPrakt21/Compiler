package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.nodes.MemoryLocation;
import compiler.codegen.llir.nodes.RegisterNode;
import compiler.codegen.llir.nodes.SideEffect;

public final class SubFromMemInstruction extends BinaryFromMemInstruction {
    public SubFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        super(bb, lhs, rhs, sideEffect);
    }

    @Override
    public String getMnemonic() {
        return "sub";
    }
}
