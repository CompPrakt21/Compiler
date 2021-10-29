package compiler.ast;

public final class UnaryExpression extends Expression {
    public enum UnaryOp {
        LogicalNot,
        Negate,
    }

    private Expression expression;
    private UnaryOp operator;

    public UnaryExpression(Expression expression, UnaryOp operator) {
        this.isError |= expression == null || operator == null;

        this.expression = expression;
        this.operator = operator;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof UnaryExpression other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression)
                && this.operator.equals(other.operator);
    }
}
