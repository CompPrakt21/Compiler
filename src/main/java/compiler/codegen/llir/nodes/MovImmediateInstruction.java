package compiler.codegen.llir.nodes;

import compiler.codegen.Register.Width;
import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovImmediateInstruction extends RegisterNode {

    private long immediateValue;
    private Register.Width width;

    public MovImmediateInstruction(BasicBlock bb, long immediateValue, Register.Width width) {
        super(bb);
        this.immediateValue = immediateValue;
        this.width = width;
        this.initTargetRegister(width);
    }

    public MovImmediateInstruction(BasicBlock bb, long immediateValue, Register target, Register.Width width) {
        super(bb);
        this.immediateValue = immediateValue;
        this.width = width;
        this.targetRegister = target;
    }

    public Register.Width getWidth() {
        return width;
    }

    public long getImmediateValue() {
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
