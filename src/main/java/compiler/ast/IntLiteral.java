package compiler.ast;

import compiler.syntax.Token;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class IntLiteral extends Expression {
    private final String value;
    private final Optional<Token> minusToken;

    public IntLiteral(Optional<Token> minusToken, Token value) {
        super();
        this.isError |= value == null;
        setSpan(value, new OptionalWrapper(minusToken));

        this.minusToken = minusToken;

        this.value = value != null ? value.getIntLiteralContent() : null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IntLiteral other)) {
            return false;
        }
        return this.value.equals(other.value);
    }

    public String getValue() {
        return value;
    }

    public Optional<Token> getMinusToken() {
        return this.minusToken;
    }
}
