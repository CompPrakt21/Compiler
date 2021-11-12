package compiler.ast;

import compiler.Token;

import java.util.List;

public final class FieldAccessExpression extends Expression {
    private Expression target;
    private Identifier identifier;

    public FieldAccessExpression(Expression target, Token dot, Token identifier) {
        super();
        this.isError |= target == null || dot == null || identifier == null;

        setSpan(target, dot, identifier);

        this.target = target;
        this.identifier = new Identifier(identifier);
    }

    public Expression getTarget() {
        return target;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    @Override
    public List<AstNode> getChildren() {
        return List.of(target);
    }

    @Override
    public String getName() {
        return identifier.getContent();
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
