package compiler.ast;

import compiler.Span;

public abstract class AstNode {
    protected Span span;

    public abstract boolean syntacticEq(AstNode otherAst);
}
