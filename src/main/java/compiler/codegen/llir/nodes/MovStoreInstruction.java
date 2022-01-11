package compiler.codegen.llir.nodes;

import compiler.codegen.Register.Width;
import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovStoreInstruction extends LlirNode implements SideEffect {
    private SideEffect sideEffect;
    private RegisterNode addrNode;
    private RegisterNode valueNode;
    private Register.Width width;

    public MovStoreInstruction(BasicBlock bb, SideEffect sideEffect, RegisterNode addrNode, RegisterNode valueNode, Register.Width width) {
        super(bb);
        this.sideEffect = sideEffect;
        this.addrNode = addrNode;
        this.width = width;
        assert addrNode.getTargetRegister().getWidth() == Register.Width.BIT64;
        this.valueNode = valueNode;
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public RegisterNode getAddrNode() {
        return addrNode;
    }

    public RegisterNode getValueNode() {
        return valueNode;
    }

    public Register.Width getWidth() {
        return this.width;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.of(sideEffect.asLlirNode(), this.addrNode, this.valueNode));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 3;
    }

    @Override
    public String getMnemonic() {
        return "mov-store";
    }
}
