package compiler.ast;

import compiler.Span;

import java.util.List;

public abstract class AstNode {
    protected Span span;

    public abstract List<AstNode> getChildren();

    public abstract String getName();

}
