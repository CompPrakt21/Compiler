package compiler.ast;

import compiler.syntax.Token;

public final class ClassType extends Type {
    private final Identifier identifier;

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
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ClassType other)) {
            return false;
        }
        return this.identifier.equals(other.identifier);
    }
}
