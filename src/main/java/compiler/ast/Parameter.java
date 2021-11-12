package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class Parameter extends AstNode implements VariableDefinition {
    private final Type type;
    private final Identifier identifier;

    public Parameter(Type type, Token identifier) {
        super();
        this.isError |= type == null || identifier == null;

        setSpan(type, identifier);

        this.type = type;
        this.identifier = new Identifier(identifier);
    }

    @Override
    public Type getType() {
        return type;
    }

    public Identifier getIdentifier() {
        return identifier;
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
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Parameter other)) {
            return false;
        }
        return this.type.syntacticEq(other.type)
                && this.identifier.equals(other.identifier);
    }
}
