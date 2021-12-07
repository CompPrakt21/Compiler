package compiler.codegen;

import java.util.List;

public class LlirGraph {
    public BasicBlock startBlock;

    public LlirGraph(List<? extends Register> argumentRegs) {
        this.startBlock = new BasicBlock("start");

        for (var reg: argumentRegs) {
            this.startBlock.addInput(reg);
        }
    }

    public BasicBlock getStartBlock() {
        return this.startBlock;
    }
}
