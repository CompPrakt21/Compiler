package compiler.ast;

import compiler.Token;

import java.util.List;

public final class ThisExpression extends Expression {
    public ThisExpression(Token thisToken) {
        super();
        setSpan(thisToken);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof ThisExpression;
    }
}
