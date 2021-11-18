package compiler.semantic.resolution;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Stack;

public class SymbolTable<T> {
    private record StackEntry<T>(String name, T def, Optional<T> prev) {
    }

    private final Stack<StackEntry<T>> stack;
    private final Stack<Integer> scopes;

    private final Map<String, T> cache;

    public SymbolTable() {
        this.stack = new Stack<>();
        this.scopes = new Stack<>();
        this.cache = new HashMap<>();
    }

    public void enterScope() {
        this.scopes.add(this.stack.size());
    }

    public void leaveScope() {
        var scopeStart = this.scopes.isEmpty() ? 0 : this.scopes.pop();

        while (this.stack.size() > scopeStart) {
            var oldEntry = this.stack.pop();

            if (oldEntry.prev.isPresent()) {
                var oldDef = oldEntry.prev.get();
                this.cache.put(oldEntry.name, oldDef);
            } else {
                this.cache.remove(oldEntry.name);
            }
        }
    }

    public void insert(String s, T def) {
        var old = this.lookupDefinition(s);
        this.stack.add(new StackEntry<>(s, def, old));
        this.cache.put(s, def);
    }

    public Optional<T> lookupDefinition(String s) {
        return Optional.ofNullable(this.cache.get(s));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SymbolTable[\n");

        for (var x : this.cache.entrySet()) {
            sb.append(String.format("\t%s -> %s\n", x.getKey(), x.getValue()));
        }

        sb.append("]");

        return sb.toString();
    }
}
