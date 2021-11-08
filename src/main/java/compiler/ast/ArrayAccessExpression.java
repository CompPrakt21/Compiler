package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class ArrayAccessExpression extends Expression {
    private Expression target;
    private Expression indexExpression;

    public ArrayAccessExpression(Expression target, Token openBracket, Expression indexExpression, Token closedBracket) {
        super();
        this.isError |= target == null || indexExpression == null || openBracket == null || closedBracket == null;
        setSpan(target, openBracket, indexExpression, closedBracket);

        this.target = target;
        this.indexExpression = indexExpression;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(target);
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

    public Expression getTarget() {
        return target;
    }

    public Expression getIndexExpression() {
        return indexExpression;
    }
}
