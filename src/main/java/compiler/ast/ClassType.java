package compiler.ast;

import compiler.Token;

import java.util.List;

public final class ClassType extends Type {
    private String identifier;

    public ClassType(Token identifier) {
        this.isError |= identifier == null;
        setSpan(identifier);

        this.identifier = identifier != null ? identifier.getIdentContent() : null;
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
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
