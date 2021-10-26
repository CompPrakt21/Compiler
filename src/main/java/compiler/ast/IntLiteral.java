package compiler.ast;

public final class IntLiteral extends Expression {
    private int value;

    public IntLiteral(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IntLiteral other)) {
            return false;
        }
        return this.value == other.value;
    }
}
