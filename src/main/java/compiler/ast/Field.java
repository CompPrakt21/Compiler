package compiler.ast;

import compiler.syntax.Token;
import compiler.syntax.TokenType;

public final class Field extends AstNode implements VariableDefinition {
    private final Identifier identifier;

    private final Type type;

    public Field(Token publicToken, Type type, Token identifier, Token semicolon) {
        super();
        assert identifier == null || identifier.type == TokenType.Identifier;
        this.isError |= identifier == null || type == null || semicolon == null;
        setSpan(publicToken, type, identifier, semicolon);

        this.identifier = new Identifier(identifier);
        this.type = type;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Field other)) {
            return false;
        }
        return this.identifier.equals(other.identifier) && this.type.syntacticEq(other.type);
    }
}
