package compiler.semantic;

import compiler.ast.AstNode;

import java.util.HashMap;
import java.util.Optional;

public class SparseAstData<T> extends AstData<T> {
    private HashMap<Integer, T> data;

    public SparseAstData() {
        this.data = new HashMap<>();
    }

    @Override
    public Optional<T> get(AstNode a) {
        return Optional.ofNullable(this.data.get(a.getID()));
    }

    @Override
    public void set(AstNode a, T t) {
        this.data.put(a.getID(), t);
    }
}
