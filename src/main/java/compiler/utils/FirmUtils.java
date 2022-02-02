package compiler.utils;

import firm.BackEdges;
import firm.Mode;
import firm.bindings.binding_irnode;
import firm.nodes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class FirmUtils {
    public static <T> List<T> toList(Iterable<T> xs) {
        List<T> result = new ArrayList<>();
        xs.forEach(result::add);
        return result;
    }

    public static List<BackEdges.Edge> backEdges(Node n) {
        return toList(BackEdges.getOuts(n));
    }

    public static List<Node> backEdgeTargets(Node n) {
        return backEdges(n).stream().map(e -> e.node).collect(Collectors.toList());
    }

    public static List<Node> preds(Node n) {
        return toList(n.getPreds());
    }

    public static void setPreds(Node n, List<Node> preds) {
        binding_irnode.set_irn_in(n.ptr, preds.size(), Node.getBufferFromNodeList(preds.toArray(i -> new Node[i])));
    }

    public static List<Node> blockContent(Block b) {
        return backEdgeTargets(b).stream()
                .filter(n -> n.getBlock().equals(b) && !(n instanceof NoMem))
                .collect(Collectors.toList());
    }

    public static Proj getCondTrueProj(Cond c) {
        return (Proj) backEdges(c).stream()
                .filter(edge -> edge.node instanceof Proj proj && proj.getNum() == 1)
                .findFirst()
                .orElseThrow().node;
    }

    public static Proj getCondFalseProj(Cond c) {
        return (Proj) backEdges(c).stream()
                .filter(edge -> edge.node instanceof Proj proj && proj.getNum() == 0)
                .findFirst()
                .orElseThrow().node;
    }

    public static Proj getOtherCondProj(Proj p) {
        assert p.getMode().equals(Mode.getX());
        var cond = (Cond) p.getPred();

        // Is the given proj, the true projection.
        if (p.getNum() == 1) {
            // If so return the other projection.
            return getCondFalseProj(cond);
        } else {
            return getCondTrueProj(cond);
        }
    }

    public static void removePred(Node n, int idx) {
        assert idx < n.getPredCount();
        var newPreds = new ArrayList<Node>();
        for (int i = 0; i < n.getPredCount(); i++) {
            if (i == idx) continue;
            newPreds.add(n.getPred(i));
        }
        setPreds(n, newPreds);
    }
}
