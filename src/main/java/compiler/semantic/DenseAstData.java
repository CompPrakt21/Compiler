package compiler.semantic;

import compiler.ast.AstNode;

import java.util.ArrayList;
import java.util.Optional;

public class DenseAstData<T> extends AstData<T> {
    private final ArrayList<T> data;

    public DenseAstData() {
        this.data = new ArrayList<>();
    }

    public void set(AstNode a, T data) {
        var id = a.getID();

        extendDataList(id + 1);

        this.data.set(id, data);
    }

    public Optional<T> get(AstNode a) {
        var id = a.getID();

        if (id < data.size()) {
            return Optional.ofNullable(this.data.get(id));
        } else {
            return Optional.empty();
        }
    }

    private void extendDataList(int length) {
        while (this.data.size() < length) {
            this.data.add(null);
        }
    }
}
