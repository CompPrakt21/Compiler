package compiler.ast;

public final class ArrayAccessExpression extends Expression {
    private Expression target;
    private Expression indexExpression;

    public ArrayAccessExpression(Expression target, Expression indexExpression) {
        this.target = target;
        this.indexExpression = indexExpression;
    }
}
