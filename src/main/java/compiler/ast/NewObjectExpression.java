package compiler.ast;

public final class NewObjectExpression extends Expression {
    private String typeIdentifier;

    public NewObjectExpression(String typeIdentifier) {
        this.typeIdentifier = typeIdentifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof NewObjectExpression other)) {
            return false;
        }
        return this.typeIdentifier.equals(other.typeIdentifier);
    }
}
