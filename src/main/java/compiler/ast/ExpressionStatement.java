package compiler.ast;

public final class ExpressionStatement extends Statement {
    private Expression expression;

    public ExpressionStatement(Expression expression) {
        this.expression = expression;
    }
}
