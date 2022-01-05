package compiler.codegen.sir;

import compiler.codegen.sir.instructions.Instruction;

import java.util.List;
import java.util.Objects;

public class BasicBlock {
    private final String label;
    private List<Instruction> instructions;

    public BasicBlock(String label, List<Instruction> instructions) {
        this.label = label;
        this.instructions = instructions;
    }

    public String getLabel() {
        return label;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicBlock that = (BasicBlock) o;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}
