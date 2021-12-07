package compiler.codegen;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract sealed class LlirNode permits RegisterNode, EffectNode, ControlFlowNode {
    private static long nextId = 1;

    private final long id;

    protected Optional<LlirNode> schedulePredecessor;

    public LlirNode() {
        this.id = nextId;
        nextId += 1;

        this.schedulePredecessor = Optional.empty();
    }

    public long getID() {
        return this.id;
    }

    public abstract Stream<LlirNode> getPreds();

    public abstract int getPredSize();

    public Optional<LlirNode> getSchedulePredecessor() {
        return this.schedulePredecessor;
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
