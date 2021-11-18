package compiler.ast;

import compiler.syntax.Token;

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

    private final Expression lhs;
    private final BinaryOp operator;
    private final String operatorRepr;
    private final Expression rhs;

    public BinaryOpExpression(Expression lhs, Token operator, Expression rhs) {
        super();
        this.isError |= lhs == null || operator == null || rhs == null;
        setSpan(lhs, operator, rhs);

        var op = switch (operator.type) {
            case Or -> BinaryOp.Or;
            case And -> BinaryOp.And;
            case Equals -> BinaryOp.Equal;
            case NotEquals -> BinaryOp.NotEqual;
            case GreaterThan -> BinaryOp.Greater;
            case GreaterThanOrEquals -> BinaryOp.GreaterEqual;
            case LessThan -> BinaryOp.Less;
            case LessThanOrEquals -> BinaryOp.LessEqual;
            case Add -> BinaryOp.Addition;
            case Subtract -> BinaryOp.Subtraction;
            case Multiply -> BinaryOp.Multiplication;
            case Divide -> BinaryOp.Division;
            case Modulo -> BinaryOp.Modulo;
            case null, default -> null;
        };

        this.lhs = lhs;
        this.operator = op;
        this.operatorRepr = operator.type.repr;
        this.rhs = rhs;
    }

    public Expression getLhs() {
        return lhs;
    }

    public BinaryOp getOperator() {
        return operator;
    }

    public Expression getRhs() {
        return rhs;
    }

    public String getOperatorRepr() {
        return operatorRepr;
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
