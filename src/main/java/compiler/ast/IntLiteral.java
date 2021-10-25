package compiler.ast;

public final class IntLiteral extends Expression {
    private int value;

    public IntLiteral(int value) {
        this.value = value;
    }
}
