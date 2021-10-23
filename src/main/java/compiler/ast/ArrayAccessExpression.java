package compiler.ast;

public final class ArrayAccessExpression extends Expression {
    private Expression indexExpression;
    public ArrayAccessExpression (Expression indexExpression) {
        this.indexExpression = indexExpression;
    }
}
