package compiler.codegen.llir.nodes;

import compiler.codegen.Register.Width;
import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovLoadInstruction extends RegisterNode implements SideEffect {
    private SideEffect sideEffect;
    private RegisterNode addrNode;
    private Register.Width width;

    public MovLoadInstruction(BasicBlock bb, SideEffect sideEffect, RegisterNode addrNode, Register.Width width) {
        super(bb);
        this.sideEffect = sideEffect;
        this.addrNode = addrNode;
        this.width = width;
        assert addrNode.getTargetRegister().getWidth() == Register.Width.BIT64;

        initTargetRegister(width);
    }

    public Register.Width getWidth() {
        return width;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.of(sideEffect.asLlirNode(), addrNode));
    }

    public RegisterNode getAddrNode() {
        return addrNode;
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2;
    }

    @Override
    public String getMnemonic() {
        return "mov-load";
    }
}
