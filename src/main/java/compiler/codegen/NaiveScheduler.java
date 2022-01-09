package compiler.codegen;

import compiler.codegen.llir.*;
import compiler.codegen.llir.nodes.InputNode;
import compiler.codegen.llir.nodes.LlirNode;
import compiler.codegen.llir.nodes.MemoryInputNode;

import java.util.*;

public class NaiveScheduler {
    private List<LlirNode> scheduleList;
    private final LlirAttribute<Visited> visited;
    private final Map<BasicBlock, List<LlirNode>> schedule;

    private final LlirGraph graph;

    private NaiveScheduler(LlirGraph graph) {
        this.scheduleList = new ArrayList<>();
        this.visited = new LlirAttribute<>();
        this.schedule = new HashMap<>();
        this.graph = graph;
    }

    private enum Visited {
        VISITED
    }

    private void scheduleBasicBlock(BasicBlock bb) {

        for (var node : bb.getOutputNodes()) {
            this.scheduleNode(node);
        }
        this.scheduleNode(bb.getEndNode());

        this.schedule.put(bb, this.scheduleList);
        this.scheduleList = new ArrayList<>();
    }

    private void scheduleNode(LlirNode node) {
        if (this.visited.contains(node) || node instanceof InputNode || node instanceof MemoryInputNode) {
            return;
        }
        this.visited.set(node, Visited.VISITED);

        var preds = node.getPreds().toList();

        for (var pred : preds) {
            this.scheduleNode(pred);
        }

        this.scheduleList.add(node);
    }

    private void schedule() {
        for (var bb : this.graph.collectAllBasicBlocks()) {
            this.scheduleBasicBlock(bb);
        }
    }

    public static ScheduleResult schedule(LlirGraph graph) {
        var scheduler = new NaiveScheduler(graph);

        scheduler.schedule();

        return new ScheduleResult(scheduler.schedule);
    }
}
