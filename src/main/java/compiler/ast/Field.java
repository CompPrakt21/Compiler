package compiler.ast;

import compiler.Token;
import compiler.TokenType;

import java.util.ArrayList;
import java.util.List;

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
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        return temp;
    }

    @Override
    public String getName() {
        return identifier.getContent();
    }

    @Override
    public boolean startsNewBlock() {
        return false;
    }

    @Override
    public String getVariable() {
        return identifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Field other)) {
            return false;
        }
        return this.identifier.equals(other.identifier) && this.type.syntacticEq(other.type);
    }
}
