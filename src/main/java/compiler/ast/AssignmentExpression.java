package compiler.ast;

public final class AssignmentExpression extends Expression {
    private Expression lftexpression;
    private Expression rgtExpression;

    public AssignmentExpression(Expression lftexpression, Expression rgExpression) {
        this.lftexpression = lftexpression;
        this.rgtExpression = rgExpression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof AssignmentExpression other)) {
            return false;
        }
        return this.lftexpression.syntacticEq(other.lftexpression)
                && this.rgtExpression.syntacticEq(other.rgtExpression);
    }
}
