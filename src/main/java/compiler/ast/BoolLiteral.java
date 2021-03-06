package compiler.ast;

import compiler.syntax.Token;

public final class BoolLiteral extends Expression {
    private final boolean value;

    public BoolLiteral(Token value) {
        super();
        this.isError |= value == null;
        setSpan(value);

        this.value = value != null && switch (value.type) {
            case True -> true;
            case False, null -> false;
            default -> throw new AssertionError("Invalid bool token.");
        };
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof BoolLiteral other)) {
            return false;
        }
        return this.value == other.value;
    }
}
