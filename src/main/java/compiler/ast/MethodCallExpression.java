package compiler.ast;

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
}
