package compiler.ast;

public final class FieldAccessExpression extends Expression {
    private Expression target;
    private String identifier;

    public FieldAccessExpression(Expression target, String identifier) {
        this.target = target;
        this.identifier = identifier;
    }
}
