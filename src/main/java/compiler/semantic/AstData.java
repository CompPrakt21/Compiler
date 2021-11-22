package compiler.semantic;

import compiler.ast.AstNode;

import java.util.Optional;

public abstract class AstData<T> {
    public abstract Optional<T> get(AstNode a);

    public abstract void set(AstNode a, T t);
}
