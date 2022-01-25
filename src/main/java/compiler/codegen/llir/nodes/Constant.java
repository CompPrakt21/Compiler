package compiler.codegen.llir.nodes;

import java.util.List;

public final class Constant implements SimpleOperand {
    private long value;

    public Constant(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
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
    public List<RegisterNode> getRegisters() {
        return List.of();
    }
}
