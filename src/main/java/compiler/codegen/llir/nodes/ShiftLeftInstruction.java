package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

public final class ShiftLeftInstruction extends ShiftInstruction {
    public ShiftLeftInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb, lhs, rhs);
    }

    @Override
    public String getMnemonic() {
        return "shl";
    }
}
