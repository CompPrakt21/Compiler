package compiler.codegen.sir;

import java.util.List;

public class SirGraph {
    private final BasicBlock startBlock;

    private final List<BasicBlock> blocks;

    public SirGraph(BasicBlock startBlock, List<BasicBlock> blocks) {
        this.startBlock = startBlock;
        this.blocks = blocks;
    }

    public BasicBlock getStartBlock() {
        return startBlock;
    }

    public List<BasicBlock> getBlocks() {
        return this.blocks;
    }
}
