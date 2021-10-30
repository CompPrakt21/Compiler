package compiler.ast;

import compiler.Token;
import compiler.TokenType;

import java.util.ArrayList;
import java.util.List;

public final class BinaryOpExpression extends Expression {
    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(lhs);
        temp.add(rhs);
        return temp;
    }

    @Override
    public String getName() {
        return operator.name();
    }

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

    public BinaryOpExpression(Expression lhs, Token operator, Expression rhs) {
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
            default -> throw new AssertionError("Only call this function with binary op tokens.");
        };

        this.lhs = lhs;
        this.operator = op;
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
