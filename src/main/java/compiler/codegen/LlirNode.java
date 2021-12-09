package compiler.codegen;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract sealed class LlirNode permits RegisterNode, EffectNode, ControlFlowNode {
    private static long nextId = 1;

    private final long id;

    protected Optional<LlirNode> scheduleNext;

    protected BasicBlock basicBlock;

    public LlirNode() {
        this.id = nextId;
        nextId += 1;

        this.scheduleNext = Optional.empty();
    }

    protected void inferBasicBlock() {
        if (this.basicBlock != null) {
            return;
        }

        Optional<BasicBlock> bb = Optional.empty();

        for (LlirNode pred : this.getPreds().toArray(LlirNode[]::new)) {
            var predBB = pred.getBasicBlock();
            if (bb.isPresent()) {
                if (predBB != bb.get()) {
                    throw new IllegalArgumentException("Predecessors are in different basic blocks.");
                }
            } else {
                bb = Optional.of(predBB);
            }
        }

        if (bb.isPresent()) {
            this.basicBlock = bb.get();
        } else {
            throw new IllegalArgumentException("Node doesn't have any predecessory and needs to set its basic block.");
        }
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
