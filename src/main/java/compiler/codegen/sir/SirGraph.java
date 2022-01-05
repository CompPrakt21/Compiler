package compiler.codegen.sir;

public class SirGraph {
    private BasicBlock startBlock;

    public SirGraph(BasicBlock startBlock) {
        this.startBlock = startBlock;
    }

    public BasicBlock getStartBlock() {
        return startBlock;
    }
}
