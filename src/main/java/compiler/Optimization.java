package compiler;

import firm.Graph;
import firm.nodes.Const;
import firm.nodes.Node;

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
}
