package compiler.ast;

public final class NewArrayExpression extends Expression {
    /* Can not be ArrayType. */
    private Type type;
    private int dimensions;
    private int firstDimensionSize;
}
