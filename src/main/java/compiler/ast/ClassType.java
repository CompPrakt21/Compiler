package compiler.ast;

import java.util.List;

public final class ClassType extends Type {
    private String identifier;

    public ClassType(String identifier) {
        this.isError |= identifier == null;

        this.identifier = identifier;
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
