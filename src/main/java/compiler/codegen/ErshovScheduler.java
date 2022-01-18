package compiler.codegen;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.LlirAttribute;
import compiler.codegen.llir.LlirGraph;
import compiler.codegen.llir.nodes.InputNode;
import compiler.codegen.llir.nodes.LlirNode;
import compiler.codegen.llir.nodes.MemoryInputNode;

import java.util.*;

public class ErshovScheduler {
    private List<LlirNode> scheduleList;
    private final LlirAttribute<Visited> visited;
    private final Map<BasicBlock, List<LlirNode>> schedule;

    private final LlirAttribute<Integer> ershovNumbers;

    private final LlirGraph graph;

    private ErshovScheduler(LlirGraph graph) {
        this.scheduleList = new ArrayList<>();
        this.visited = new LlirAttribute<>();
        this.schedule = new HashMap<>();
        this.ershovNumbers = new LlirAttribute<>();
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

        var preds = new ArrayList<>(node.getPreds().toList());

        preds.sort((a, b) -> this.ershovNumbers.get(a).compareTo(this.ershovNumbers.get(b)) * -1);

        for (var pred : preds) {
            this.scheduleNode(pred);
        }

        this.scheduleList.add(node);
    }

    private void calcErshovNumbers(BasicBlock bb) {
        for (var node : bb.getOutputNodes()) {
            this.calcErshovNumberRecursive(node);
        }
        this.calcErshovNumberRecursive(bb.getEndNode());
    }

    private void calcErshovNumberRecursive(LlirNode node) {
        if (this.ershovNumbers.contains(node)) {
            return;
        }

        node.getPreds().forEach(this::calcErshovNumberRecursive);

        if (node.getPredSize() == 0) {
            this.ershovNumbers.set(node, 0);
        } else {
            var preds = new ArrayList<Integer>(node.getPreds().map(this.ershovNumbers::get).toList());

            preds.sort(Comparator.reverseOrder());

            for (int i = 0; i < preds.size(); i++) {
                preds.set(i, preds.get(i) + i);
            }

            var num = preds.stream().max(Comparator.naturalOrder()).orElseThrow();

            this.ershovNumbers.set(node, num);
        }
    }

    private void schedule() {
        for (var bb : this.graph.collectAllBasicBlocks()) {
            this.calcErshovNumbers(bb);
            this.scheduleBasicBlock(bb);
        }
    }

    public static ScheduleResult schedule(LlirGraph graph) {
        var scheduler = new ErshovScheduler(graph);

        scheduler.schedule();

        return new ScheduleResult(scheduler.schedule);
    }
}
