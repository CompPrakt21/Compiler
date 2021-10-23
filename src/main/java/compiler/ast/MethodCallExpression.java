package compiler.ast;

import java.util.List;

public final class MethodCallExpression extends Expression {
    private String identifier;

    private List<Expression> arguments;
    public MethodCallExpression(String identifier, List<Expression> arguments) {
        this.identifier = identifier;
        this.arguments = arguments;
    }
}
