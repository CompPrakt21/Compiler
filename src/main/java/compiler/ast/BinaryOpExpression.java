package compiler.ast;

import java.util.ArrayList;

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
}
