package compiler.ast;

import compiler.Token;

import java.util.List;

public final class NullExpression extends Expression {
    public NullExpression(Token nullToken) {
        super();
        setSpan(nullToken);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof NullExpression;
    }
}
