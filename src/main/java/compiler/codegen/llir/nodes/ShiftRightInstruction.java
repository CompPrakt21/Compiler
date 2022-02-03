package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class ShiftRightInstruction extends ShiftInstruction {
    public ShiftRightInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "shr";
    }
}
