package compiler.ast;

public final class BinaryOpExpression extends Expression {
    public enum BinaryOp {
        And,
        Or,
        Equal,
        NotEqual,
        Less,
        LessEqual,
        Greater,
        GreaterEqual,
        Addition,
        Subtraction,
        Multiplication,
        Division,
        Modulo,
    }

    private Expression lhs;
    private BinaryOp operator;
    private Expression rhs;

    public BinaryOpExpression(Expression lhs, BinaryOp operator, Expression rhs) {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof BinaryOpExpression other)) {
            return false;
        }
        return this.lhs.syntacticEq(other.lhs)
                && this.operator.equals(other.operator)
                && this.rhs.syntacticEq(other.rhs);
    }
}
