package compiler.ast;

import java.util.List;

public final class ClassType extends Type {
    private String identifier;

    public ClassType(String identifier) {
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
}
