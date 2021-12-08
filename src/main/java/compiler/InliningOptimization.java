package compiler;

import firm.BackEdges;
import firm.Graph;
import firm.nodes.Node;

import java.util.ArrayDeque;

public class InliningOptimization {

    private Graph g;
    private DataFlow dataFlow = new DataFlow();

    public InliningOptimization(Graph g) {
        this.g = g;
    }

    private void collectNodes() {
        BackEdges.enable(g);
        ArrayDeque<Node> worklist = new ArrayDeque<>();
        NodeCollector c = new NodeCollector(worklist);
        g.walkTopological(c);

    }


}
