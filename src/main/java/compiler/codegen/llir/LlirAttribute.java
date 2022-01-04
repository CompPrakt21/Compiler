package compiler.codegen.llir;

import compiler.codegen.llir.nodes.LlirNode;

import java.util.HashMap;
import java.util.Optional;

public class LlirAttribute<T> {
    private HashMap<Long, T> data;

    public LlirAttribute() {
        this.data = new HashMap<>();
    }

    public void set(LlirNode node, T value) {
        this.data.put(node.getID(), value);
    }

    public T get(LlirNode node) {
        var value = this.data.get(node.getID());
        assert value != null;
        return value;
    }

    public Optional<T> tryGet(LlirNode node) {
        return Optional.ofNullable(this.data.get(node.getID()));
    }

    public boolean contains(LlirNode node) {
        return this.data.containsKey(node.getID());
    }
}
