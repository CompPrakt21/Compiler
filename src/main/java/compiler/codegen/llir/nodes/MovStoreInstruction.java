package compiler.codegen.llir.nodes;

import compiler.codegen.Register.Width;
import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class MovStoreInstruction extends LlirNode implements SideEffect {
    private SideEffect sideEffect;
    private MemoryLocation addr;
    private RegisterNode valueNode;
    private Register.Width width;

    public MovStoreInstruction(BasicBlock bb, SideEffect sideEffect, MemoryLocation addr, RegisterNode valueNode, Register.Width width) {
        super(bb);
        this.sideEffect = sideEffect;
        this.addr = addr;
        this.width = width;
        this.valueNode = valueNode;
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public MemoryLocation getAddress() {
        return addr;
    }

    public RegisterNode getValueNode() {
        return valueNode;
    }

    public Register.Width getWidth() {
        return this.width;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(sideEffect.asLlirNode(), this.valueNode), this.addr.getRegisters().stream()));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2 + this.addr.getRegisters().size();
    }

    @Override
    public String getMnemonic() {
        return "mov-store";
    }
}
