package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class UnaryExpression extends Expression {
    public enum UnaryOp {
        LogicalNot,
        Negate,
    }

    private final Expression expression;
    private final UnaryOp operator;
    private final String operatorRepr;

    public UnaryExpression(Expression expression, Token operator) {
        super();
        this.isError |= expression == null || operator == null;

        setSpan(expression, operator);

        this.expression = expression;
        this.operator = operator != null ? switch (operator.type) {
            case Not -> UnaryOp.LogicalNot;
            case Subtract -> UnaryOp.Negate;
            default -> throw new AssertionError("Unreacheable, because parser doesn't call with different token");
        } : null;
        this.operatorRepr = operator != null ? operator.type.repr : null;
    }

    public Expression getExpression() {
        return expression;
    }

    public UnaryOp getOperator() {
        return operator;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof UnaryExpression other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression)
                && this.operator.equals(other.operator);
    }

    public String getOperatorRepr() {
        return operatorRepr;
    }

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
}
