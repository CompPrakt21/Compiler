package compiler.codegen.llir;

import java.util.stream.Stream;

public final class MovLoadInstruction extends RegisterNode implements SideEffect {
    private SideEffect sideEffect;
    private RegisterNode addrNode;

    public MovLoadInstruction(BasicBlock bb, SideEffect sideEffect, RegisterNode addrNode) {
        super(bb);
        this.sideEffect = sideEffect;
        this.addrNode = addrNode;
        initTargetRegister();
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
