package compiler.ast;

public final class BoolLiteral extends Expression {
    private boolean value;

    public BoolLiteral(boolean value) {
        this.value = value;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof BoolLiteral other)) {
            return false;
        }
        return this.value == other.value;
    }
}
