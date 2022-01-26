package compiler.codegen.sir;

import java.util.ArrayList;
import java.util.List;

public class SirGraph {
    private final BasicBlock startBlock;

    /**
     * Every basic block in the graph.
     * Before register allocation, they are in no particular order.
     */
    private final List<BasicBlock> blocks;

    /**
     * All instructions of the graph are linearized and therefore can be given
     * an index.
     * However, since we remain in a control flow graph we give each block the instruction index
     * of its first instruction so that we can reconstruct where the instruction of a given instruction
     * is.
     */
    private final List<Integer> startInstructionIndices;

    public SirGraph(BasicBlock startBlock, List<BasicBlock> blocks) {
        this.startBlock = startBlock;
        this.blocks = new ArrayList<>(blocks);
        this.startInstructionIndices = new ArrayList<>();
    }

    public void recalculateInstructionIndices() {
        this.startInstructionIndices.clear();

        int startIndex = 0;
        for (var bb : this.blocks) {
            this.startInstructionIndices.add(startIndex);
            startIndex += bb.getInstructions().size();
        }
    }

    public List<Integer> getStartInstructionIndices() {
        if (this.startInstructionIndices.size() != this.blocks.size()) {
            this.recalculateInstructionIndices();
        }

        return this.startInstructionIndices;
    }

    public BasicBlock getStartBlock() {
        return startBlock;
    }

    public List<BasicBlock> getBlocks() {
        return this.blocks;
    }

}
