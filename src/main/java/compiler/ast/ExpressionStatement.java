package compiler.ast;

public final class ExpressionStatement extends Statement {
    private Expression expression;

    public ExpressionStatement(Expression expression) {
        this.expression = expression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ExpressionStatement other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression);
    }
}
