package compiler.ast;

import java.util.ArrayList;
import java.util.List;
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
}
