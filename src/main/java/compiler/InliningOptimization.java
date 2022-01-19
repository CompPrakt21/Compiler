package compiler;

import firm.*;
import firm.nodes.*;

import java.util.*;
import java.util.stream.StreamSupport;

public class InliningOptimization {

    record Pair<X, Y>(X first, Y second){}

    private Graph graph;
    private ArrayDeque<Node> worklist = new ArrayDeque<>();

    private ArrayList<Graph> activatedGraphs = new ArrayList<>();
    private final int MAX_COPY_SIZE = 26;



    private int size = 0;


    private ArrayList<Call> callNodes = new ArrayList<>();
    private ArrayList<Proj> projNodes = new ArrayList<>();


    public InliningOptimization(Graph g) {
        graph = g;
        if (!BackEdges.enabled(graph))
            BackEdges.enable(graph);
        activatedGraphs.add(graph);
        NodeCollector c = new NodeCollector(worklist);
        graph.walkTopological(c);
        size = worklist.size();
    }

    public int getSize() {
        return size;
    }

    private void findAllAddressCalls(ArrayDeque<Node> worklist) {

        worklist.stream().filter(node -> node instanceof Proj proj && proj.getMode().equals(Mode.getIs())).forEach(node -> projNodes.add((Proj) node));
        worklist.stream().filter(node -> node instanceof Call).forEach(node -> callNodes.add((Call) node));
        ArrayList<Node> toRemove = new ArrayList<>();
        for (Call callNode : callNodes) {
            ArrayList<Node> temp = new ArrayList<>();
            callNode.getPreds().forEach(node -> temp.add(node));
            if (temp.stream().anyMatch(node -> node instanceof Address address && address.getEntity().getName().matches("(_System_out_(write|println|read|flush))|__builtin_alloc_function__"))) {
                toRemove.add(callNode);
            }
        }
        callNodes.removeAll(toRemove);
        suitabilityFilter();

    }

    private void suitabilityFilter() {
        ArrayList<Proj> projTemp = new ArrayList<>();
        projNodes.stream().filter(proj -> proj.getPred() instanceof Proj proj1 && callNodes.contains(proj1.getPred())).forEach(projTemp::add);
        this.projNodes = projTemp;

    }

    private Pair<Node, Proj> copyIntoGraph(ArrayList<Node> listOfToBeCopied, Block targetBlock, Pair<Node, Proj> returns) {
        Node returnNode;
        Proj returnProj;


        Graph graph = targetBlock.getGraph();
        HashMap<Node, Node> copied = new HashMap<>();
        for (Node node : listOfToBeCopied) {
            Node temp = node.copyInto(graph);
            temp.setBlock(targetBlock);
            copied.put(node, temp);
        }

        Node[] set = new Node[copied.size()];
        copied.values().toArray(set);
        for (int i = 0; i < set.length; i++) {
            Iterator<Node> te = set[i].getPreds().iterator();
            Node next;
            while(te.hasNext()) {
                next = te.next();
                if (copied.containsKey(next))
                    set[i].setPred(i, copied.get(next));
            }
        }
        returnNode = copied.get(returns.first);
        returnProj = (Proj) copied.get(returns.second);

        return new Pair<>(returnNode, returnProj);
    }

    private void inline(Optional<Proj> resultNode, Node aboveMemoryNodeBeforeAddressCall, Proj belowMemoryNodeAfterAddressCall, ArrayList<Node> sourceParameters, ArrayDeque<Node> remoteParameters, Node remoteResultNode, Proj remoteLastMemoryNode, Proj remoteFirstMemoryNode, Call callNode ) {

        Iterator<Node> predsIterator = callNode.getPreds().iterator();
         for (int i = 0; i < callNode.getPredCount(); i++) {
            Node node = predsIterator.next();
            if (node.equals(resultNode.get())) {
                callNode.setPred(i, remoteResultNode);
            } else if  (node.equals(belowMemoryNodeAfterAddressCall)) {
                callNode.setPred(i, remoteLastMemoryNode);
            }
        }
        remoteFirstMemoryNode.setPred(aboveMemoryNodeBeforeAddressCall);
        ArrayList<Node> parameterChildren = new ArrayList<>();
        remoteParameters.stream()
                .forEach(node -> BackEdges.getOuts(node).forEach(edge -> {
                    if (!parameterChildren.contains(edge.node))
                        parameterChildren.add(edge.node);
                }));
        for (Node parameterChild : parameterChildren) {
            Iterator<Node> preds = parameterChild.getPreds().iterator();
            int i = 0;
            Node pred;
            while (preds.hasNext()) {
                pred = preds.next();
                if (remoteParameters.contains(pred) && parameterChild.getGraph().equals(sourceParameters.get(i).getGraph()))
                    parameterChild.setPred(i, sourceParameters.get(i));
                i++;
            }
        }
    }

    public void collectNodes() {

        findAllAddressCalls(worklist);

        for (Call callNode : callNodes) {
            ArrayList<Node> temp = new ArrayList<>();
            Optional<Proj> resultNode = null; //can be empty
            Node aboveMemoryNodeBeforeAddressCall = null;
            Proj belowMemoryNodeAfterAddressCall = null;
            ArrayList<Node> sourceParameters = new ArrayList<>();
            ArrayDeque<Node> remoteParameters = new ArrayDeque<>();
            Node remoteResultNode = null;
            Proj remoteLastMemoryNode = null;
            Proj remoteFirstMemoryNode = null;
            Proj varStorageNode;
            Return targetReturn = null;
            Graph curTargetGraph = null;
            int curTargetGraphSize = Integer.MAX_VALUE;

            callNode.getPreds().forEach(node -> {
                temp.add(node);
            });

            for (Node node : temp) {
                switch (node) {
                    case Proj proj -> {
                        if (proj.getMode().equals(Mode.getM()))
                            aboveMemoryNodeBeforeAddressCall = proj.getPred();
                        else if (proj.getMode().equals(Mode.getP()))
                            varStorageNode = proj;
                        else
                            sourceParameters.add(proj);

                    }
                    case Address address -> {
                        curTargetGraph = address.getEntity().getGraph();
                        if (!BackEdges.enabled(curTargetGraph)) {
                            BackEdges.enable(curTargetGraph);
                            activatedGraphs.add(curTargetGraph);
                        }
                        if (!curTargetGraph.equals(graph)) {
                            InliningOptimization optimization = new InliningOptimization(curTargetGraph);
                            optimization.collectNodes();
                            curTargetGraphSize = optimization.getSize();
                        }
                        if (curTargetGraphSize > MAX_COPY_SIZE)
                            break;


                        targetReturn = (Return) curTargetGraph.getEndBlock().getPred(0);
                        ArrayList<Node> toBeCopied = DFSGraph(targetReturn, remoteParameters);
                        Pair<Node, Proj> unCopied = new Pair<>(targetReturn.getPred(1), (Proj) targetReturn.getPred(0));
                        Pair<Node, Proj> copiedItems = copyIntoGraph(toBeCopied, (Block) callNode.getBlock(), unCopied);
                        remoteResultNode = copiedItems.first;
                        remoteLastMemoryNode = copiedItems.second;
                        Node tempNode = remoteLastMemoryNode;
                        while (tempNode.getPred(0).getGraph().equals(remoteLastMemoryNode.getGraph()) && !(tempNode.getPred(0) instanceof Start)) {
                            tempNode = tempNode.getPred(0);
                        }
                        remoteFirstMemoryNode = (Proj) tempNode;
                    }
                    default -> {
                        sourceParameters.add(node);
                    }
                }
            }
            Call belowCallNode = null;
            for (BackEdges.Edge edge : BackEdges.getOuts(callNode)) {
                Node node = edge.node;
                if (node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
                    belowMemoryNodeAfterAddressCall = proj;
                    belowCallNode = (Call) BackEdges.getOuts(proj).iterator().next().node;
                }
                else if (node instanceof Proj proj && proj.getMode().equals(Mode.getT()))
                    resultNode = projNodes.stream().filter(proj1 -> proj1.getPred().equals(proj)).findFirst();
            }

            if (curTargetGraphSize < MAX_COPY_SIZE)
                inline(resultNode, aboveMemoryNodeBeforeAddressCall, belowMemoryNodeAfterAddressCall, sourceParameters, remoteParameters, remoteResultNode, remoteLastMemoryNode, remoteFirstMemoryNode, belowCallNode);

        }

    }

    //TODO: store which nodes are first and last
    private ArrayList<Node> DFSGraph(Node start, ArrayDeque<Node> parameters) {
        ArrayList<Node> result = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        Node curr = start;

        do {
            Iterator<Node> i = curr.getPreds().iterator();
            i.forEachRemaining(node -> {
                boolean flag = true;
                if (node instanceof Proj proj && node.getMode().equals(Mode.getIs())) {
                    flag = false;
                    parameters.add(node);
                }
                else if (node instanceof Start) {
                    flag = false;
                    if (!stack.isEmpty() && !stack.contains(node)) {
                        stack.add(0, node);
                    }
                }
                else if (!result.contains(node)) result.add(node);
                if (flag && !stack.contains(node)) stack.add(node);
            });
            curr = stack.pop();
        } while (!stack.isEmpty() && !(curr instanceof Return));

        return result;
    }


}
