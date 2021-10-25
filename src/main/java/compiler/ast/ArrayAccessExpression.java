package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class ArrayAccessExpression extends Expression {
    private Expression target;
    private Expression indexExpression;

    public ArrayAccessExpression(Expression target, Expression indexExpression) {
        this.target = target;
        this.indexExpression = indexExpression;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(indexExpression);
        return temp;
    }

    @Override
    public String getName() {
        return "ArrayAccess";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ArrayAccessExpression other)) {
            return false;
        }
        return this.target.syntacticEq(other.target)
                && this.indexExpression.syntacticEq(other.indexExpression);
    }
}
