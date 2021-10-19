package compiler.ast;

public final class UnaryExpression extends Expression {
    public enum UnaryOp {
        LogicalNot,
        Negate,
    }

    private Expression expression;
    private UnaryOp operator;
}
