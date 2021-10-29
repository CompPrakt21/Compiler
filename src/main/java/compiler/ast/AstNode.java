package compiler.ast;

import compiler.Span;

import java.util.List;

public abstract class AstNode {
    protected Span span;
    protected boolean isError;

    public abstract List<AstNode> getChildren();

    public abstract String getName();


    public abstract boolean syntacticEq(AstNode otherAst);

    public boolean isError() {
        return this.isError;
    }

    public <T> T makeError(boolean isError) {
        this.isError |= isError;
        return (T) this;
    }
}
