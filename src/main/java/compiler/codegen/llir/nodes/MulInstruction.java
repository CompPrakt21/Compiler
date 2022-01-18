package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class MulInstruction extends BinaryInstruction {
    public MulInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "imul";
    }
}
