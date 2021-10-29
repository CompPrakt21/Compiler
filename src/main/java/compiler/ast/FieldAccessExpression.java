package compiler.ast;

import java.util.List;

public final class FieldAccessExpression extends Expression {
    private Expression target;
    private String identifier;

    public FieldAccessExpression(Expression target, String identifier) {
        this.isError |= target == null || identifier == null;

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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof FieldAccessExpression other)) {
            return false;
        }
        return this.identifier.equals(other.identifier)
                && target.syntacticEq(other.target);
    }
}
