package compiler.ast;

import java.util.List;

public final class Reference extends Expression {
    private String identifier;

    public Reference(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public List<AstNode> getChildren() {
        return List.of();
    }

    @Override
    public String getName() {
        return this.identifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Reference other)) {
            return false;
        }
        return this.identifier.equals(other.identifier);
    }
}
