package compiler.codegen.llir;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Stream;

public class DumpLlir {
    private final PrintWriter out;
    private final HashMap<LlirNode, String> visitedNodes;
    private final HashSet<BasicBlock> visitedBasicBlocks;

    private final Stack<BasicBlock> dumpStack;

    // We collect all edges first so the nodes that appear in them don't
    // interfere with the subgraphs.
    private final List<String> edges;

    public DumpLlir(PrintWriter out) {
        this.out = out;
        this.visitedNodes = new HashMap<>();
        this.visitedBasicBlocks = new HashSet<>();
        this.dumpStack = new Stack<>();
        this.edges = new ArrayList<>();
    }

    public void dump(LlirGraph graph) {
        this.visitedNodes.clear();

        out.format("digraph {\n");
        out.println("\trankdir=\"BT\"");
        out.println("\tcompound=true");

        this.dumpStack.add(graph.getStartBlock());

        // Collect dot nodes and edges
        while (!this.dumpStack.isEmpty()) {
            var bb = this.dumpStack.pop();
            this.visitedBasicBlocks.add(bb);
            this.dumpBasicBlock(bb);
        }

        // do the actual dumping
        for (var bb : this.visitedBasicBlocks) {
            this.out.format("subgraph cluster%s {\n", bb.getLabel());
            this.out.format("label=\"%s\"", bb.getLabel());

            for (var in : bb.getInputNodes()) {
                this.out.println(this.visitedNodes.get(in));
            }

            for (var out: bb.getOutputNodes()) {
                if (out instanceof InputNode in && bb.getInputNodes().contains(in)) continue;
                this.out.println(this.visitedNodes.get(out));
            }
            this.out.println(this.visitedNodes.get(bb.getEndNode()));

            for (var n: bb.getAllNodes()) {
                if (!bb.getOutputNodes().contains(n) && !bb.getInputNodes().contains(n) && bb.getEndNode() != n) {
                    this.out.println(this.visitedNodes.get(n));
                }
            }

            this.out.println("}");
        }

        for (var edge: this.edges) {
            this.out.println(edge);
        }

        out.println("}");

        out.flush();
    }

    private void dumpBasicBlock(BasicBlock bb) {
        if (!bb.isFinished()) {
            throw new IllegalCallerException("Can't dump basic block during construction.");
        }

        this.dumpNodeRecursive(bb.getEndNode());

        for (var out : bb.getOutputNodes()) {
            this.dumpNodeRecursive(out);
        }
    }

    private void dumpNodeRecursive(LlirNode node) {
        if (!this.visitedNodes.containsKey(node)) {
            this.dumpNode(node);

            node.getPreds().forEach(this::dumpNodeRecursive);

            getPredsWithLabel(node).forEach(pred -> {
                this.edges.add(String.format("\t%s -> %s [label=\"%s\"]", node.getID(), pred.node.getID(), pred.label));
            });

            if (node.getScheduleNext().isPresent()) {
                this.edges.add(String.format("\t%s -> %s[color=purple, style=dashed]", node.getID(), node.getScheduleNext().get().getID()));
            }

            if (node instanceof ControlFlowNode controlFlowNode) {
                for (var bb : controlFlowNode.getTargets()) {

                    if (!this.visitedBasicBlocks.contains(bb)) {
                        this.dumpStack.push(bb);
                    }

                    var label = switch (controlFlowNode) {
                        case BranchInstruction b -> b.getTrueBlock() == bb ? "true" : "false";
                        default -> "";
                    };

                    var t = getHeighestRankedNode(bb);
                    this.edges.add(String.format("\t%s -> %s [ltail=cluster%s, color=red, dir=back, headlabel=\"%s\"]", t.getID(), controlFlowNode.getID(), bb.getLabel(), label));
                }
            }
        }
    }

    private void dumpNode(LlirNode node) {
        var isOutput = node instanceof RegisterNode regNode && regNode.getBasicBlock().getOutputNodes().contains(regNode);
        var isInput = node instanceof InputNode || node instanceof MemoryInputNode;

        var shape = isInput ? "hexagon" : isOutput ? "ellipse" : "box";

        var color = switch (node) {
            case ControlFlowNode ignored -> "red";
            case SideEffect ignored -> "cyan";
            case MovImmediateInstruction ignored -> "orange";
            default -> "black";
        };

        String label = switch (node) {
            case InputNode inode -> inode.getTargetRegister().getName();
            case RegisterNode regNode -> String.format("%s <- %s", regNode.getTargetRegister().getName(), getNodeLabel(node));
            default -> getNodeLabel(node);
        };

        this.visitedNodes.put(node, String.format("\t%s[label=\"%s\", shape=%s, color=%s]", node.getID(), label, shape, color));
    }

    private static String getNodeLabel(LlirNode node) {
        return switch (node) {
            case MovImmediateInstruction mov -> String.format("%s 0x%x", mov.getMnemonic(), mov.getImmediateValue());
            case CallInstruction call -> String.format("%s %s", call.getMnemonic(), call.getCalledMethod() != null ? call.getCalledMethod().getLinkerName() : "null");
            default -> node.getMnemonic();
        };
    }

    private static LlirNode getHeighestRankedNode(BasicBlock bb) {
        var ranks = new HashMap<LlirNode, Integer>();
        var queue = new ArrayDeque<LlirNode>();

        ranks.put(bb.getEndNode(), 0);
        bb.getOutputNodes().forEach(o -> ranks.put(o, 0));
        queue.add(bb.getEndNode());
        queue.addAll(bb.getOutputNodes());

        while (!queue.isEmpty()) {
            var node = queue.pop();
            var rank = ranks.get(node);

            var predRank = rank + 1;
            for(var pred : (Iterable<LlirNode>)node.getPreds()::iterator) {
                if (!ranks.containsKey(pred) || ranks.get(pred) < predRank) {
                    ranks.put(pred, predRank);
                    queue.add(pred);
                }
            }
        }

        return ranks.keySet().stream().max(Comparator.comparingInt(ranks::get)).orElseThrow();
    }

    record PredWithLabel(LlirNode node, String label) {}

    private static Stream<PredWithLabel> getPredsWithLabel(LlirNode node) {
        return switch (node) {
            case BinaryInstruction a -> Stream.of(new PredWithLabel(a.getLhs(), "lhs"), new PredWithLabel(a.getRhs(), "rhs"));
            case CmpInstruction a -> Stream.of(new PredWithLabel(a.getLhs(), "lhs"), new PredWithLabel(a.getRhs(), "rhs"));
            default -> node.getPreds().map(l -> new PredWithLabel(l, ""));
        };
    }
}
