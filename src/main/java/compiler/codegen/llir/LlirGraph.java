package compiler.codegen.llir;

import java.util.*;

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

    public Collection<BasicBlock> collectAllBasicBlocks() {
        List<BasicBlock> bbs = new ArrayList<>();
        ArrayDeque<BasicBlock> queue = new ArrayDeque<>();
        bbs.add(this.startBlock);
        queue.push(this.startBlock);

        while (!queue.isEmpty()) {
            for (var target : queue.pop().getEndNode().getTargets()) {
                if (!bbs.contains(target)) {
                    bbs.add(target);
                    queue.add(target);
                }
            }
        }

        return bbs;
    }

    public VirtualRegister.Generator getVirtualRegGenerator() {
        return virtualRegGenerator;
    }

    public BasicBlock getStartBlock() {
        return this.startBlock;
    }
}
