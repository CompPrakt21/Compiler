package compiler.codegen;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class DumpLlir {
    private final PrintWriter out;
    private HashSet<LlirNode> visitedNodes;

    private HashSet<BasicBlock> visitedBasicBlocks;

    private Stack<BasicBlock> dumpStack;

    // We collect all edges first so the nodes that appear in them don't
    // interfere with the subgraphs.
    private List<String> edges;

    public DumpLlir(PrintWriter out) {
        this.out = out;
        this.visitedNodes = new HashSet<>();
        this.visitedBasicBlocks = new HashSet<>();
        this.dumpStack = new Stack<>();
        this.edges = new ArrayList<>();
    }

    public void dump(LlirGraph graph) {
        this.visitedNodes.clear();

        out.format("digraph {\n");
        out.println("\trankdir=\"BT\"");
        out.println("\tcompound=true");
        out.println("\tnode[ordering=out]");

        this.dumpStack.add(graph.getStartBlock());

        while (!this.dumpStack.isEmpty()) {
            var bb = this.dumpStack.pop();
            this.visitedBasicBlocks.add(bb);
            this.dumpBasicBlock(bb);
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

        var isStartBlock = bb.getGraph().getStartBlock() == bb;

        out.format("subgraph cluster%s {\n", bb.getLabel());
        out.format("\tlabel=\"%s%s\"\n", bb.getLabel(), isStartBlock ? "<start>" : "");

        this.dumpNodeRecursive(bb.getEndNode());

        for (var out : bb.getOutputNodes()) {
            this.dumpNodeRecursive(out);
        }

        out.println("}");
    }

    private void dumpNodeRecursive(LlirNode node) {
        if (!this.visitedNodes.contains(node)) {
            this.visitedNodes.add(node);

            this.dumpNode(node);

            node.getPreds().forEach(this::dumpNodeRecursive);

            node.getPreds().forEach(pred -> {
                this.edges.add(String.format("\t%s -> %s", node.getID(), pred.getID()));
            });

            if (node.getScheduleNext().isPresent()) {
                this.edges.add(String.format("\t%s -> %s[color=purple, style=dashed]", node.getID(), node.getScheduleNext().get().getID()));
            }

            if (node instanceof ControlFlowNode controlFlowNode) {
                for (var bb : controlFlowNode.getTargets()) {

                    if (!this.visitedBasicBlocks.contains(bb)) {
                        this.dumpStack.push(bb);
                        this.edges.add(String.format("\t%s -> %s [lhead=cluster%s, color=red]", node.getID(), bb.getEndNode().getID(), bb.getLabel()));
                    }
                }
            }
        }
    }

    private void dumpNode(LlirNode node) {
        var isOutput = node instanceof RegisterNode regNode && regNode.getBasicBlock().getOutputNodes().contains(regNode);
        var isInput = node instanceof InputNode;

        var shape = isInput || isOutput ? "ellipse" : "box";

        String label = switch (node) {
            case InputNode inode -> inode.getTargetRegister().getName();
            case RegisterNode regNode -> String.format("%s <- %s", regNode.getTargetRegister().getName(), getNodeLabel(node));
            default -> getNodeLabel(node);
        };

        this.out.format("\t%s[label=\"%s\", shape=%s]\n", node.getID(), label, shape);
    }

    private static String getNodeLabel(LlirNode node) {
        return switch (node) {
            case MovImmediateInstruction mov -> String.format("%s 0x%x", mov.getMnemonic(), mov.getImmediateValue());
            default -> node.getMnemonic();
        };
    }
}
