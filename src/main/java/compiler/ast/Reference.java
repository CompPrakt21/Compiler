package compiler.ast;

import compiler.Token;

import java.util.List;

public final class Reference extends Expression {
    private final Identifier identifier;

    public Reference(Token identifier) {
        super();
        this.isError |= identifier == null;
        setSpan(identifier);

        this.identifier = new Identifier(identifier);
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return this.identifier.getContent();
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Reference other)) {
            return false;
        }
        return this.identifier.equals(other.identifier);
    }
}
