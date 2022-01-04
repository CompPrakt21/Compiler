package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.Objects;
import java.util.stream.Stream;

public abstract sealed class LlirNode permits CmpInstruction, ControlFlowNode, MemoryInputNode, MovStoreInstruction, RegisterNode {
    private static long nextId = 1;

    private final long id;

    protected BasicBlock basicBlock;

    public LlirNode(BasicBlock bb) {
        this.id = nextId;
        nextId += 1;

        this.basicBlock = bb;
    }

    public long getID() {
        return this.id;
    }

    public BasicBlock getBasicBlock() {
        return this.basicBlock;
    }

    public abstract Stream<LlirNode> getPreds();

    public abstract int getPredSize();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlirNode llirNode = (LlirNode) o;
        return id == llirNode.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public abstract String getMnemonic();
}
