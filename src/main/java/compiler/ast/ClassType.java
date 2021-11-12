package compiler.ast;

import compiler.Token;

import java.util.List;

public final class ClassType extends Type {
    private Identifier identifier;

    public ClassType(Token identifier) {
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
        return identifier.getContent();
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ClassType other)) {
            return false;
        }
        return this.identifier.equals(other.identifier);
    }
}
