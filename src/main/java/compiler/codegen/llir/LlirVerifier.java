package compiler.codegen.llir;

import compiler.codegen.llir.nodes.AllocCallInstruction;
import compiler.codegen.llir.nodes.ControlFlowNode;
import compiler.codegen.llir.nodes.LlirNode;
import compiler.codegen.llir.nodes.MethodCallInstruction;
import compiler.types.ArrayTy;
import compiler.types.ClassTy;

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

        switch (node) {
            case ControlFlowNode cfn -> {
                assert cfn.getTargets().stream().allMatch(Objects::nonNull);
                cfn.getTargets().stream().forEach(this::verifyBasicBlock);
            }
            case AllocCallInstruction a -> {
                assert a.getTargetRegister().getWidth() == Register.Width.BIT64;
            }
            case MethodCallInstruction m -> {
                var returnTy = m.getCalledMethod().getReturnTy();

                var isWide = returnTy instanceof ClassTy || returnTy instanceof ArrayTy;

                if (isWide) {
                    assert m.getTargetRegister().getWidth() == Register.Width.BIT64;
                } else {
                    assert m.getTargetRegister().getWidth() == Register.Width.BIT32;
                }
            }
            default -> {}
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
