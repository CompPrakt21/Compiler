package compiler.ast;

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
    public BinaryOpExpression(Expression lhs, BinaryOp operator, Expression rhs) {
        this.lhs = lhs;
        this.operator = operator;
        this.rhs = rhs;
    }
}
