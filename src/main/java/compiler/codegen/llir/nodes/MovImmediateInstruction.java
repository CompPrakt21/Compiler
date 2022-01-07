package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovImmediateInstruction extends RegisterNode {

    private int immediateValue;

    public MovImmediateInstruction(BasicBlock bb, int immediateValue, Register.Width width) {
        super(bb);
        this.immediateValue = immediateValue;
        this.initTargetRegister(width);
    }

    public MovImmediateInstruction(BasicBlock bb, int immediateValue, Register target) {
        super(bb);
        this.immediateValue = immediateValue;
        this.targetRegister = target;
    }

    public int getImmediateValue() {
        return immediateValue;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return super.getPreds();
    }

    @Override
    public int getPredSize() {
        return super.getPredSize();
    }

    @Override
    public String getMnemonic() {
        return "mov-imm";
    }
}
