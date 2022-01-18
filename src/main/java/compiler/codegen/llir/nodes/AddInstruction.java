package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class AddInstruction extends BinaryInstruction {
    public AddInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "add";
    }
}
