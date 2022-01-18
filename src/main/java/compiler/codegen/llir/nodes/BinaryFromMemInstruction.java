package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public abstract sealed class BinaryFromMemInstruction extends RegisterNode implements SideEffect permits AddFromMemInstruction, MulFromMemInstruction, SubFromMemInstruction, XorFromMemInstruction {
    private RegisterNode lhs;
    private MemoryLocation rhs;

    private SideEffect sideEffect;

    public BinaryFromMemInstruction(BasicBlock bb, RegisterNode lhs, MemoryLocation rhs, SideEffect sideEffect) {
        super(bb);

        this.lhs = lhs;
        this.rhs = rhs;
        this.sideEffect = sideEffect;

        this.initTargetRegister(lhs.getTargetRegister());
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public RegisterNode getLhs() {
        return this.lhs;
    }

    public MemoryLocation getRhs() {
        return this.rhs;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(lhs, sideEffect.asLlirNode()), rhs.getRegisters().stream()));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2 + rhs.getRegisters().size();
    }
}
