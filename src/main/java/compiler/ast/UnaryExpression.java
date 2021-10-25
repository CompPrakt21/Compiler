package compiler.ast;

public final class UnaryExpression extends Expression {
    public enum UnaryOp {
        LogicalNot,
        Negate,
    }

    private Expression expression;
    private UnaryOp operator;
    public UnaryExpression(Expression expression, UnaryOp operator) {
        this.expression = expression;
        this.operator = operator;
    }
}
