package compiler.ast;

public final class NewObjectExpression extends Expression {
    private String typeIdentifier;
    public NewObjectExpression(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
    }
}
