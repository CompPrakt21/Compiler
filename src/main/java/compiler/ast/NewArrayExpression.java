package compiler.ast;

public final class NewArrayExpression extends Expression {
    /* Can not be ArrayType. */
    private Type type;
    private Expression firstDimensionSize;
    private int dimensions;

    public NewArrayExpression(Type type, Expression firstDimensionSize, int dimensions) {
        this.type = type;
        this.dimensions = dimensions;
        this.firstDimensionSize = firstDimensionSize;
    }
}
