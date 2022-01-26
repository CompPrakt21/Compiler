package compiler.utils;

import firm.BackEdges;
import firm.bindings.binding_irnode;
import firm.nodes.Block;
import firm.nodes.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
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
                .filter(n -> n.getBlock().equals(b))
                .collect(Collectors.toList());
    }
}
