package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class ExpressionStatement extends Statement {
    private Expression expression;

    public ExpressionStatement(Expression expression) {
        this.isError |= expression == null;

        this.expression = expression;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(expression);
        return temp;
    }

    @Override
    public String getName() {
        return "ExpressionStatement";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ExpressionStatement other)) {
            return false;
        }
        return this.expression.syntacticEq(other.expression);
    }
}
