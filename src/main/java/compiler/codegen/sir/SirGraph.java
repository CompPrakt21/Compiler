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

    public SirGraph(BasicBlock startBlock, List<BasicBlock> blocks) {
        this.startBlock = startBlock;
        this.blocks = new ArrayList<>(blocks);
    }

    public BasicBlock getStartBlock() {
        return startBlock;
    }

    public List<BasicBlock> getBlocks() {
        return this.blocks;
    }

}
