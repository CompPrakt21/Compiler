package compiler;

import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.nodes.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Optimization {
    public static Graph constantFolding(Graph g) {
        Map<Node, DataFlow.ConstantValue> values = DataFlow.analyzeConstantFolding(g);
        record Change(Node node, int predIdx, Node folded) { }
        List<Change> changes = new ArrayList<>();
        for (Node n : values.keySet()) {
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                DataFlow.ConstantValue v = values.get(pred);
                if (v == null) {
                    continue;
                }
                Node folded = switch (v) {
                    case DataFlow.Unknown u -> g.newConst(0, pred.getMode());
                    case DataFlow.Constant c -> g.newConst(c.value);
                    case DataFlow.Variable vx -> pred;
                    default -> throw new AssertionError("Did not expect null here");
                };
                if (pred.getMode().equals(folded.getMode())) {
                    changes.add(new Change(n, i, folded));
                }
            }
        }
        for (Change c : changes) {
            c.node.setPred(c.predIdx, c.folded);
        }
        return g;
    }

    public static Graph eliminateRedundantSideEffects(Graph g) {
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        g.walkTopological(new NodeCollector(nodes));
        BackEdges.enable(g);
        for (Node n : nodes) {
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                if (!(pred.getMode().equals(Mode.getM()) && pred instanceof Proj proj)) {
                    continue;
                }
                Node divOrMod = proj.getPred();
                record D(Node left, Node right, Node mem) { }
                D d;
                switch (divOrMod) {
                    case Div div -> d = new D(div.getLeft(), div.getRight(), div.getMem());
                    case Mod mod -> d = new D(mod.getLeft(), mod.getRight(), mod.getMem());
                    default -> {
                        continue;
                    }
                };
                if (!(d.left instanceof Const && d.right instanceof Const divisor)) {
                    continue;
                }
                if (divisor.getTarval().asInt() == 0) {
                    continue;
                }
                List<Node> outs = new ArrayList<>();
                BackEdges.getOuts(divOrMod).forEach(e -> outs.add(e.node));
                if (outs.size() > 1) {
                    continue;
                }
                n.setPred(i, d.mem);
            }
        }
        BackEdges.disable(g);
        return g;
    }

    public static Graph eliminateRedundantPhis(Graph g) {
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        g.walkTopological(new NodeCollector(nodes));
        for (Node n : nodes) {
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                if (!(pred instanceof Phi p)) {
                    continue;
                }
                List<Node> in = new ArrayList<>();
                p.getPreds().forEach(in::add);
                assert in.size() > 0;
                Node first = in.get(0);
                if (!in.stream().allMatch(input -> first.equals(input))) {
                    continue;
                }
                n.setPred(i, first);
            }
        }
        return g;
    }
}
