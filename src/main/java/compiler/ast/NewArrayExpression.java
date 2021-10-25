package compiler.ast;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        temp.add(firstDimensionSize);
        return temp;
    }

    @Override
    public String getName() {
        return "Array " + dimensions + " and first " + firstDimensionSize;
    }
}
