package compiler.ast;

import compiler.syntax.Token;

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
