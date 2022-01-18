package compiler.codegen.llir.nodes;

import java.util.List;

public final class Constant implements SimpleOperand {
    private int value;

    public Constant(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
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
    public List<RegisterNode> getRegisters() {
        return List.of();
    }
}
