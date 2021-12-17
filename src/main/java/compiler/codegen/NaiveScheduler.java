package compiler.codegen;

import compiler.codegen.llir.*;

import java.util.*;
import java.util.stream.Collectors;

public class NaiveScheduler {
    private final List<LlirNode> scheduleList;
    private final Map<BasicBlock, LlirNode> startNodes;
    private final LlirAttribute<Visited> visited;
    private final LlirAttribute<LlirNode> schedule;

    private final LlirGraph graph;

    private NaiveScheduler(LlirGraph graph) {
        this.scheduleList = new ArrayList<>();
        this.startNodes = new HashMap<>();
        this.visited = new LlirAttribute<>();
        this.schedule = new LlirAttribute<>();
        this.graph = graph;
    }

    private enum Visited {
        VISITED;
    }

    private void scheduleBasicBlock(BasicBlock bb) {
        this.scheduleList.clear();

        for (var node : bb.getOutputNodes()) {
            this.scheduleNode(node);
        }
        this.scheduleNode(bb.getEndNode());

        for (int i = 0; i < this.scheduleList.size() - 1; i++) {
            this.schedule.set(this.scheduleList.get(i), this.scheduleList.get(i + 1));
        }

        this.startNodes.put(bb, this.scheduleList.get(0));
    }

    private void scheduleNode(LlirNode node) {
        if (this.visited.contains(node) || node instanceof InputNode || node instanceof MemoryInputNode) {
            return;
        }
        this.visited.set(node, Visited.VISITED);

        var preds = node.getPreds().collect(Collectors.toList());

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

    public record ScheduleResult(
        LlirAttribute<LlirNode> schedule,
        Map<BasicBlock, LlirNode> startNodes
    ){}

    public static ScheduleResult schedule(LlirGraph graph) {
        var scheduler = new NaiveScheduler(graph);

        scheduler.schedule();

        return new ScheduleResult(scheduler.schedule, scheduler.startNodes);
    }
}
