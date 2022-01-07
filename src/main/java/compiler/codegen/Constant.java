package compiler.codegen;

public final class Constant extends Operand {
    private int value;

    public Constant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }

    @Override
    public String formatIntelSyntax() {
        return Integer.toString(value);
    }

    @Override
    public String formatATTSyntax() {
        return String.format("$%d", value);
    }
}
