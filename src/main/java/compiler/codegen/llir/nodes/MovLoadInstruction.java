package compiler.codegen.llir.nodes;

import compiler.codegen.Register.Width;
import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovLoadInstruction extends RegisterNode implements SideEffect {
    private SideEffect sideEffect;
    private MemoryLocation address;
    private Register.Width width;

    public MovLoadInstruction(BasicBlock bb, SideEffect sideEffect, MemoryLocation address, Register.Width width) {
        super(bb);
        this.sideEffect = sideEffect;
        this.address = address;
        this.width = width;

        initTargetRegister(width);
    }

    public Register.Width getWidth() {
        return width;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(sideEffect.asLlirNode()), this.address.getRegisters().stream()));
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public MemoryLocation getAddress() {
        return address;
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 1 + this.address.getRegisters().size();
    }

    @Override
    public String getMnemonic() {
        return "mov-load";
    }
}
