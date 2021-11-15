package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class ExpressionStatement extends Statement {
    private final Expression expression;

    public ExpressionStatement(Expression expression, Token semicolon) {
        super();
        this.isError |= expression == null || semicolon == null;
        setSpan(expression, semicolon);

        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ExpressionStatement other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression);
    }
}
