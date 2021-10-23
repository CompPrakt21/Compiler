package compiler.ast;

public final class FieldAccessExpression extends Expression {
    private String identifier;
    public FieldAccessExpression (String identifier) {
        this.identifier = identifier;
    }
}
