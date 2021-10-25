package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class UnaryExpression extends Expression {
    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(expression);
        return temp;
    }

    @Override
    public String getName() {
        return operator.name();
    }

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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof UnaryExpression other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression)
                && this.operator.equals(other.operator);
    }
}
