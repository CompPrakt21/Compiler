package compiler.codegen;

import java.util.List;

public final class Constant extends Operand {
    private long value;

    public Constant(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }

    @Override
    public String formatIntelSyntax() {
        return Long.toString(value);
    }

    @Override
    public String formatATTSyntax() {
        return String.format("$%d", value);
    }

    @Override
    public List<Register> getRegisters() {
        return List.of();
    }
}
