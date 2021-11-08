package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class Parameter extends AstNode {
    private Type type;
    private String identifier;

    public Parameter(Type type, Token identifier) {
        super();
        this.isError |= type == null || identifier == null;

        setSpan(type, identifier);

        this.type = type;
        this.identifier = identifier != null ? identifier.getIdentContent() : null;
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
        if (!(otherAst instanceof Parameter other)) {
            return false;
        }
        return this.type.syntacticEq(other.type)
                && this.identifier.equals(other.identifier);
    }

    public Type getType() {
        return type;
    }

    public String getIdentifier() {
        return identifier;
    }
}
