package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AssignmentExpression extends Expression {
    private Expression lftexpression;
    private Optional<Expression> rgtExpression;
    public AssignmentExpression(Expression lftexpression, Optional<Expression> rgExpression) {
        this.lftexpression = lftexpression;
        this.rgtExpression = rgExpression;
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
