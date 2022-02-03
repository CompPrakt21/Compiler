package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class AndInstruction extends BinaryInstruction {
    public AndInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "and";
    }
}
