package compiler.ast;

import compiler.syntax.Token;

public final class NewArrayExpression extends Expression {
    // Structure: new type[firstDimensionSize]([])^(dimensions - 1)
    /* Can not be ArrayType. */
    private final Type type;
    private final Expression firstDimensionSize;
    private final int dimensions;

    public NewArrayExpression(Token newToken, Type type, Expression firstDimensionSize, int dimensions, Token lastBracket) {
        super();
        this.isError |= type == null || firstDimensionSize == null || lastBracket == null || newToken == null;

        setSpan(newToken, type, firstDimensionSize, lastBracket);

        this.type = type;
        this.dimensions = dimensions;
        this.firstDimensionSize = firstDimensionSize;
    }

    public Type getType() {
        return type;
    }

    public Expression getFirstDimensionSize() {
        return firstDimensionSize;
    }

    public int getDimensions() {
        return dimensions;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof NewArrayExpression other)) {
            return false;
        }
        return this.type.syntacticEq(other.type)
                && this.firstDimensionSize.syntacticEq(other.firstDimensionSize)
                && this.dimensions == other.dimensions;
    }
}
