package compiler.codegen.llir;

import java.util.Objects;

/**
 * This utility class checks some invariants of a llir graph to notice potential errors
 * as soon as possible.
 *
 * Currently it checks the following invariants:
 *
 * - All nodes have non-null dependencies.
 * - A nodes dependencies are in the same basic block.
 */
public class LlirVerifier {
    private final LlirGraph graph;

    // Singleton element
    enum Visited {
        Visited;
    }

    private final LlirAttribute<Visited> visited;

    private LlirVerifier(LlirGraph graph) {
        this.graph = graph;
        this.visited = new LlirAttribute<>();
    }

    private void verify() {
        this.verifyBasicBlock(this.graph.getStartBlock());
    }

    private void verifyNode(LlirNode node) {
        if (this.visited.contains(node)) {
            return;
        } else{
            this.visited.set(node, Visited.Visited);
        }
        assert node.getPreds().allMatch(Objects::nonNull);
        assert node.getPreds().allMatch(pred -> node.getBasicBlock() == pred.getBasicBlock());

        node.getPreds().forEach(this::verifyNode);

        if (node instanceof ControlFlowNode cfn) {
            assert cfn.getTargets().stream().allMatch(Objects::nonNull);
            cfn.getTargets().stream().forEach(this::verifyBasicBlock);
        }
    }

    private void verifyBasicBlock(BasicBlock bb) {
        this.verifyNode(bb.getEndNode());
        bb.getOutputNodes().stream().forEach(this::verifyNode);
    }

    public static void verify(LlirGraph graph) {
        new LlirVerifier(graph).verify();
    }
}
