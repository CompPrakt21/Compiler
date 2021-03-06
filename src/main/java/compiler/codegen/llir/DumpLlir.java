package compiler.codegen.llir;

import compiler.codegen.ScheduleResult;
import compiler.codegen.llir.nodes.*;

import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class DumpLlir {
    private final PrintWriter out;
    private final HashMap<LlirNode, String> visitedNodes;
    private final HashSet<BasicBlock> visitedBasicBlocks;

    private Optional<ScheduleResult> schedule;

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
        this.schedule = Optional.empty();
    }

    public DumpLlir withSchedule(ScheduleResult schedule) {
        this.schedule = Optional.of(schedule);
        return this;
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

            if (this.schedule.isPresent()) {
                var schedule = this.schedule.get().schedule().get(node.getBasicBlock());
                var nodeScheduleIdx = schedule.indexOf(node);

                if (nodeScheduleIdx != -1) {
                    var targetScheduleIdx = nodeScheduleIdx + 1;

                    if (targetScheduleIdx < schedule.size()) {
                        var target = schedule.get(targetScheduleIdx);
                        this.edges.add(String.format("\t%s -> %s[color=purple, style=dashed, constraint=false]", node.getID(), target.getID()));
                    }
                }
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

        this.visitedNodes.put(node, String.format("\t%s[label=\"%s\t#%s\", shape=%s, color=%s]", node.getID(), label, node.getID(), shape, color));

        node.getScheduleDependencies().forEach(pred -> this.edges.add(String.format("%s -> %s [style=dotted]", node.getID(), pred.getID())));
    }

    private static String getNodeLabel(LlirNode node) {
        return switch (node) {
            case MovImmediateInstruction mov -> String.format("%s 0x%x", mov.getMnemonic(), mov.getImmediateValue());
            case MethodCallInstruction call -> String.format("%s %s", call.getMnemonic(), call.getCalledMethod().getLinkerName());
            case AllocCallInstruction call -> String.format("%s <alloc>", call.getMnemonic());
            case BinaryFromMemInstruction bin -> String.format("%s %s %s", bin.getMnemonic(), bin.getLhs().getTargetRegister(), bin.getRhs().formatIntelSyntax());
            case BinaryInstruction bin -> String.format("%s %s %s", bin.getMnemonic(), bin.getLhs().getTargetRegister(), bin.getRhs().formatIntelSyntax());
            case LoadEffectiveAddressInstruction lea -> String.format("%s %s", lea.getMnemonic(), lea.getLoc().formatIntelSyntax());
            case MovLoadInstruction mov -> String.format("%s %s", mov.getMnemonic(), mov.getAddress().formatIntelSyntax());
            case MovStoreInstruction mov -> String.format("%s %s %s", mov.getMnemonic(), mov.getAddress().formatIntelSyntax(), mov.getValueNode().getTargetRegister());
            case CmpFromMemInstruction cmp -> String.format("%s %s %s", cmp.getMnemonic(), cmp.getLhs().getTargetRegister(), cmp.getRhs().formatIntelSyntax());
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
            case BinaryInstruction a -> Stream.concat(Stream.of(new PredWithLabel(a.getLhs(), "lhs")), a.getRhs().getRegisters().stream().map(r -> new PredWithLabel(r, "rhs")));
            case BinaryFromMemInstruction a -> Stream.concat(Stream.of(new PredWithLabel(a.getLhs(), "lhs"), new PredWithLabel(a.getSideEffect().asLlirNode(), "mem")), a.getRhs().getRegisters().stream().map(r -> new PredWithLabel(r, "rhs")));
            case CmpInstruction a -> Stream.concat(Stream.of(new PredWithLabel(a.getLhs(), "lhs")), a.getRhs().getRegisters().stream().map(reg -> new PredWithLabel(reg, "rhs")));
            case CmpFromMemInstruction a -> Stream.concat(Stream.of(new PredWithLabel(a.getLhs(), "lhs"), new PredWithLabel(a.getSideEffect().asLlirNode(), "mem")), a.getRhs().getRegisters().stream().map(reg -> new PredWithLabel(reg, "rhs")));
            case MovStoreInstruction mov -> Stream.concat(Stream.of(new PredWithLabel(mov.getSideEffect().asLlirNode(), "mem"), new PredWithLabel(mov.getValueNode(), "val")), mov.getAddress().getRegisters().stream().map(reg -> new PredWithLabel(reg, "")));
            case MovLoadInstruction mov -> Stream.concat(Stream.of(new PredWithLabel(mov.getSideEffect().asLlirNode(), "mem")), mov.getAddress().getRegisters().stream().map(reg -> new PredWithLabel(reg, "")));
            case CallInstruction call -> {
                var result = new ArrayList<PredWithLabel>();
                result.add(new PredWithLabel(call.getSideEffect().asLlirNode(), "mem"));

                for (int i = 0; i < call.getArguments().size(); i++) {
                    result.add(new PredWithLabel(call.getArguments().get(i), String.format("a%s", i)));
                }

                yield result.stream();
            }
            default -> node.getPreds().map(l -> new PredWithLabel(l, ""));
        };
    }
}
