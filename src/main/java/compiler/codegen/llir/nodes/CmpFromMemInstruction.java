package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class CmpFromMemInstruction extends LlirNode implements SideEffect, CmpLikeInstruction {
    private SideEffect sideEffect;
    private RegisterNode lhs;
    private MemoryLocation rhs;
    private boolean hasReversedArguments;

    public CmpFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, boolean reversedArguments, SideEffect sideEffect) {
        super(bb);
        this.lhs = lhs;
        this.rhs = rhs;
        this.sideEffect = sideEffect;
        this.hasReversedArguments = reversedArguments;
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public RegisterNode getLhs() {
        return lhs;
    }

    public MemoryLocation getRhs() {
        return rhs;
    }

    @Override
    public boolean hasReversedArguments() {
        return hasReversedArguments;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(lhs, sideEffect.asLlirNode()), rhs.getRegisters().stream()));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2 + rhs.getRegisters().size();
    }

    @Override
    public String getMnemonic() {
        return "cmp";
    }
}
