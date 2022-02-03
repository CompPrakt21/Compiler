package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class ArithmeticShiftRightInstruction extends ShiftInstruction {
    public ArithmeticShiftRightInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "sal";
    }
}
