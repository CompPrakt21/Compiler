package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.BranchInstruction;
import compiler.codegen.sir.instructions.ControlFlowInstruction;
import compiler.codegen.sir.instructions.JumpInstruction;
import compiler.codegen.sir.instructions.ReturnInstruction;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class BlockSchedule {
    /**
     * Schedules all basic blocks in reverse postfix order.
     * The resulting schedule is stored in graph.blocks
     */
    public static void scheduleReversePostorder(SirGraph graph) {
        HashSet<BasicBlock> visitedBlocks = new HashSet<>();

        graph.getBlocks().clear();

        scheduleBlockPostOrder(graph.getStartBlock(), visitedBlocks, graph.getBlocks());

        Collections.reverse(graph.getBlocks());
        graph.recalculateInstructionIndices();
    }

    private static void scheduleBlockPostOrder(BasicBlock bb, HashSet<BasicBlock> visited, List<BasicBlock> blockSequence) {
        if (visited.contains(bb)) {
            return;
        }

        visited.add(bb);

        var lastInstr = (ControlFlowInstruction) bb.getInstructions().get(bb.getInstructions().size() - 1);
        switch (lastInstr) {
            case JumpInstruction jump -> scheduleBlockPostOrder(jump.getTarget(), visited, blockSequence);
            case BranchInstruction branch -> {
                scheduleBlockPostOrder(branch.getFalseBlock(), visited, blockSequence);
                scheduleBlockPostOrder(branch.getTrueBlock(), visited, blockSequence);
            }
            case ReturnInstruction ignored -> {}
        }

        blockSequence.add(bb);
    }
}
