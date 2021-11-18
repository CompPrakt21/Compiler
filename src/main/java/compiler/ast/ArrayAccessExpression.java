package compiler.ast;

import compiler.syntax.Token;

public final class ArrayAccessExpression extends Expression {
    private final Expression target;
    private final Expression indexExpression;

    public ArrayAccessExpression(Expression target, Token openBracket, Expression indexExpression, Token closedBracket) {
        super();
        this.isError |= target == null || indexExpression == null || openBracket == null || closedBracket == null;
        setSpan(target, openBracket, indexExpression, closedBracket);

        this.target = target;
        this.indexExpression = indexExpression;
    }

    public Expression getTarget() {
        return target;
    }

    public Expression getIndexExpression() {
        return indexExpression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ArrayAccessExpression other)) {
            return false;
        }
        return this.target.syntacticEq(other.target)
                && this.indexExpression.syntacticEq(other.indexExpression);
    }
}
