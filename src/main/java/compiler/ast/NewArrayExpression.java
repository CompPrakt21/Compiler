package compiler.ast;

public final class NewArrayExpression extends Expression {
    /* Can not be ArrayType. */
    private Type type;
    private int dimensions;
    private int firstDimensionSize;
    public NewArrayExpression(Type type, int dimensions, int firstDimensionSize){
        this.type = type;
        this.dimensions = dimensions;
        this.firstDimensionSize = firstDimensionSize;
    }
}
