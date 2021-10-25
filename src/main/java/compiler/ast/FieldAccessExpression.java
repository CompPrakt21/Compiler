package compiler.ast;

import java.util.List;

public final class FieldAccessExpression extends Expression {
    private Expression target;
    private String identifier;

    public FieldAccessExpression(Expression target, String identifier) {
        this.target = target;
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
