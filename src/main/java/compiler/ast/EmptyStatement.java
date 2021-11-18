package compiler.ast;

import compiler.syntax.Token;

public final class EmptyStatement extends Statement {
    public EmptyStatement(Token semicolon) {
        super();
        setSpan(semicolon);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof EmptyStatement;
    }
}
