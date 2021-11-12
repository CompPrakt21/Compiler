package compiler.ast;

import compiler.HasSpan;
import compiler.Span;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract sealed class AstNode implements HasSpan
        permits Expression, Statement, Type, Program, Class, Method, Field, Parameter, Identifier {
    protected Span span;
    protected boolean isError;
    private final int id;

    private static int next_id = 1;

    public String id;

    public abstract List<AstNode> getChildren();

    public abstract String getName();

    protected AstNode() {
        this.id = next_id;
        next_id += 1;
    }

    public int getID() {
        return this.id;
    }

    public abstract boolean syntacticEq(AstNode otherAst);

    public boolean isError() {
        return this.isError;
    }

    @SuppressWarnings("unchecked")
    public <T> T makeError(boolean isError) {
        this.isError |= isError;
        return (T) this;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public Span getSpan() {
        return this.span;
    }

    protected void setSpan(HasSpan... spans) {
        this.span = Arrays.stream(spans)
                .filter(Objects::nonNull)
                .map(HasSpan::getSpan)
                .filter(Objects::nonNull)
                .filter(span -> span.length() > 0)
                .reduce(Span::merge)
                .orElse(new Span(-1, 0));
    }
}
