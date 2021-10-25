package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class ExpressionStatement extends Statement {
    private Expression expression;

    public ExpressionStatement(Expression expression) {
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
}
