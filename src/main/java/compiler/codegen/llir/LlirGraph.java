package compiler.codegen.llir;

import java.util.List;

public class LlirGraph {
    private BasicBlock startBlock;

    private VirtualRegister.Generator virtualRegGenerator;

    private static int nextBasicBlockId = 0;

    public LlirGraph(VirtualRegister.Generator generator) {
        this.startBlock = newBasicBlock();
        this.virtualRegGenerator = generator;
    }

    public BasicBlock newBasicBlock() {
        var id = nextBasicBlockId;
        nextBasicBlockId += 1;

        var label = String.format("BB%d", id);

        return new BasicBlock(this, label);
    }

    public VirtualRegister.Generator getVirtualRegGenerator() {
        return virtualRegGenerator;
    }

    public BasicBlock getStartBlock() {
        return this.startBlock;
    }
}
