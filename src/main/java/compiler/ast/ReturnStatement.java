package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import compiler.utils.OptionalUtils;

import java.util.Optional;

public final class ReturnStatement extends Statement {
    private Optional<Expression> expression;

    public ReturnStatement(Optional<Expression> expression) {
        this.expression = expression;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(expression.get());
        return temp;
    }

    @Override
    public String getName() {
        return "ReturnStatement";
    }
    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ReturnStatement other)) {
            return false;
        }
        return OptionalUtils.combine(this.expression, other.expression, AstNode::syntacticEq).orElse(true);
    }

}
