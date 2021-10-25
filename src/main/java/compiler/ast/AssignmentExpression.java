package compiler.ast;

import java.util.Optional;

public final class AssignmentExpression extends Expression {
    private Expression lftexpression;
    private Expression rgtExpression;

    public AssignmentExpression(Expression lftexpression, Expression rgExpression) {
        this.lftexpression = lftexpression;
        this.rgtExpression = rgExpression;
    }
}
