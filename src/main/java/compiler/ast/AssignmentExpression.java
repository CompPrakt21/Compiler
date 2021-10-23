package compiler.ast;

import java.util.Optional;

public final class AssignmentExpression extends Expression {
    private Expression lftexpression;
    private Optional<Expression> rgtExpression;
    public AssignmentExpression(Expression lftexpression, Optional<Expression> rgExpression) {
        this.lftexpression = lftexpression;
        this.rgtExpression = rgExpression;
    }
}
