package compiler.ast;

public final class ArrayAccessExpression extends Expression {
    private Expression target;
    private Expression indexExpression;

    public ArrayAccessExpression(Expression target, Expression indexExpression) {
        this.isError |= target == null || indexExpression == null;

        this.target = target;
        this.indexExpression = indexExpression;
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
