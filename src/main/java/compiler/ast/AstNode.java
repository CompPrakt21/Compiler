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
    private int id;

    private static int next_id = 1;

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

    public <T> T makeError(boolean isError) {
        this.isError |= isError;
        return (T) this;
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
