package compiler.ast;

import java.util.List;

public final class MethodCallExpression extends Expression {
    private String identifier;

    private List<Expression> arguments;
}
