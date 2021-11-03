package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class NewArrayExpression extends Expression {
    /* Can not be ArrayType. */
    private Type type;
    private Expression firstDimensionSize;
    private int dimensions;

    public NewArrayExpression(Type type, Expression firstDimensionSize, int dimensions, Token lastBracket) {
        this.isError |= type == null || firstDimensionSize == null || lastBracket == null;

        setSpan(type, firstDimensionSize, lastBracket);

        this.type = type;
        this.dimensions = dimensions;
        this.firstDimensionSize = firstDimensionSize;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        temp.add(firstDimensionSize);
        return temp;
    }

    @Override
    public String getName() {
        return "Array_Size_" + dimensions;
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

    public Type getType() {
        return type;
    }

    public Expression getFirstDimensionSize() {
        return firstDimensionSize;
    }

    public int getDimensions() {
        return dimensions;
    }
}
