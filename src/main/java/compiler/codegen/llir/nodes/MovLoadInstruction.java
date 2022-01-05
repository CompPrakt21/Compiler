package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovLoadInstruction extends RegisterNode implements SideEffect {
    private SideEffect sideEffect;
    private RegisterNode addrNode;

    public MovLoadInstruction(BasicBlock bb, SideEffect sideEffect, RegisterNode addrNode, Register.Width outputWidth) {
        super(bb);
        this.sideEffect = sideEffect;
        this.addrNode = addrNode;
        assert addrNode.getTargetRegister().getWidth() == Register.Width.BIT64;

        initTargetRegister(outputWidth);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(sideEffect.asLlirNode(), addrNode);
    }

    public RegisterNode getAddrNode() {
        return addrNode;
    }

    @Override
    public int getPredSize() {
        return 2;
    }

    @Override
    public String getMnemonic() {
        return "mov-load";
    }
}
