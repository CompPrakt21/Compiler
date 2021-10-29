package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AssignmentExpression extends Expression {
    private Expression lftexpression;
    private Expression rgtExpression;

    public AssignmentExpression(Expression lftexpression, Expression rgExpression) {
        this.lftexpression = lftexpression;
        this.rgtExpression = rgExpression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof AssignmentExpression other)) {
            return false;
        }
        return this.lftexpression.syntacticEq(other.lftexpression)
                && this.rgtExpression.syntacticEq(other.rgtExpression);
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(lftexpression);
        if (!rgtExpression.isEmpty()) {
            temp.add(rgtExpression.get());
        }
        return temp;
    }

    @Override
    public String getName() {
        return "AssignmentExpresssion";
    }
}
