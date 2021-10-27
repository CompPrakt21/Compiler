package compiler.ast;

public final class IntLiteral extends Expression {
    private long value;

    public IntLiteral(long value) {
        this.value = value;
    }

    public long getValue() {
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
