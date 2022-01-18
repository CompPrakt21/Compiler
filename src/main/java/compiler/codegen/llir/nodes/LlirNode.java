package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public abstract sealed class LlirNode permits CmpFromMemInstruction, CmpInstruction, ControlFlowNode, MemoryInputNode, MovStoreInstruction, RegisterNode {
    private static long nextId = 1;

    private final long id;

    protected BasicBlock basicBlock;

    /**
     * This node can only be scheduled, after all its data dependencies and *scheduleDependices* are scheduled.
     * In this list extra nodes, which aren't data or memory dependencies can be registered.
     * We need this, for example, to ensure that registers for phis are not overwritten with their new value, before any usage of the old value..
     */
    private final List<LlirNode> scheduleDependencies;

    public LlirNode(BasicBlock bb) {
        this.id = nextId;
        nextId += 1;

        this.scheduleDependencies = new ArrayList<>();

        this.basicBlock = bb;
    }

    public long getID() {
        return this.id;
    }

    public BasicBlock getBasicBlock() {
        return this.basicBlock;
    }

    public Stream<LlirNode> getPreds() {
        return this.scheduleDependencies.stream();
    }

    public int getPredSize() {
        return this.scheduleDependencies.size();
    }

    public List<LlirNode> getScheduleDependencies() {
        return scheduleDependencies;
    }

    public void addScheduleDependency(LlirNode node) {
        this.scheduleDependencies.add(node);
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
