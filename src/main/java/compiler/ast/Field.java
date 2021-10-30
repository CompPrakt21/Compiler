package compiler.ast;

import compiler.Token;
import compiler.TokenType;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class Field extends AstNode {
    private String identifier;

    private Type type;

    public Field(Token publicToken, Type type, Token identifier) {
        assert identifier.type == TokenType.Identifier;
        this.isError |= identifier == null || type == null;
        setSpan(publicToken, type, identifier);

        this.identifier = identifier.getIdentContent();
        this.type = type;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        return temp;
    }

    @Override
    public String getName() {
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
