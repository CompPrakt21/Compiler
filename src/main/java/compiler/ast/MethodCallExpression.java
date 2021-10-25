package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MethodCallExpression extends Expression {
    private Optional<Expression> target;

    private String identifier;

    private List<Expression> arguments;

    public MethodCallExpression(Optional<Expression> target, String identifier, List<Expression> arguments) {
        this.target = target;
        this.identifier = identifier;
        this.arguments = arguments;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.addAll(arguments);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
    }
}
