package compiler.codegen.llir;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract sealed class LlirNode permits CmpInstruction, ControlFlowNode, MemoryInputNode, MovStoreInstruction, RegisterNode {
    private static long nextId = 1;

    private final long id;

    protected Optional<LlirNode> scheduleNext;

    protected BasicBlock basicBlock;

    public LlirNode(BasicBlock bb) {
        this.id = nextId;
        nextId += 1;

        this.scheduleNext = Optional.empty();
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

    public Optional<LlirNode> getScheduleNext() {
        return this.scheduleNext;
    }

    public void setScheduleNext(LlirNode next) {
        this.scheduleNext = Optional.of(next);
    }

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
