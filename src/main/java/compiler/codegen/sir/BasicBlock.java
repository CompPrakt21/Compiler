package compiler.codegen.sir;

import compiler.codegen.sir.instructions.Instruction;

import java.util.List;
import java.util.Objects;

public class BasicBlock {
    /**
     * Name of the basic block, same as in the llir usually BBxx.
     * Where xx is some number.
     */
    private final String label;

    /**
     * The linear sequence of instructions in this basic block.
     * The last instruction is always the one and only ControlFlowInstruction.
     */
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

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
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
