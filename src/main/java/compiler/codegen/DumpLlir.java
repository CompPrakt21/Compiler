package compiler.codegen;

import java.io.PrintWriter;
import java.util.HashSet;

public class DumpLlir {
    private final PrintWriter out;
    private HashSet<LlirNode> visited;

    public DumpLlir(PrintWriter out) {
        this.out = out;
        this.visited = new HashSet<LlirNode>();
    }

    public void dump(BasicBlock bb) {
        if (!bb.isFinished()) {
            throw new IllegalCallerException("Can't dump basic block during construction.");
        }

        out.format("digraph %s {\n", bb.getLabel());

        this.dumpNodeRecursive(bb.getEndNode());

        out.println("}");

        out.flush();
    }

    private void dumpNodeRecursive(LlirNode node) {
        if (!this.visited.contains(node)) {
            this.visited.add(node);

            this.out.format("\t%s[label=\"%s\"]", node.getID(), node.getMnemonic());

            node.getPreds().forEach(pred -> {
                this.out.format("\t%s -> %s\n", node.getID(), pred.getID());
            });

            node.getPreds().forEach(this::dumpNodeRecursive);
        }
    }
}
