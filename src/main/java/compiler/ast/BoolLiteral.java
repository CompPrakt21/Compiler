package compiler.ast;

public final class BoolLiteral extends Expression {
    private boolean value;

    public BoolLiteral(boolean value) {
        this.value = value;
    }
}
