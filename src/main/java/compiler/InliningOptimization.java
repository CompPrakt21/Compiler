package compiler;

import firm.*;
import firm.bindings.binding_be;
import firm.nodes.*;

import javax.swing.text.html.Option;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InliningOptimization {

    record Pair<X, Y>(X first, Y second){}

    private Graph graph;
    private ArrayDeque<Node> worklist = new ArrayDeque<>();
    private DataFlow dataFlow = new DataFlow();
    private Node predsMemoryCall = null; //needed
    private Node succsMemoryCall = null; //replaced
    private ArrayDeque<Node> parameters = new ArrayDeque<>(); //needed
    private Address addressNode = null; //replaced
    private Graph addressGraph = null;  //needed
    private Pair<Node, Integer> isNode = null; //replaced
    private Node callingProjIs = null; //needed

    private ArrayList<Graph> activatedGraphs = new ArrayList<>();


    private ArrayList<Call> callNodes = new ArrayList<>();
    private ArrayList<Proj> projNodes = new ArrayList<>();
    private ArrayList<Pair<Call, Call>> AddressCallAndClassInstantiation = new ArrayList();


    public InliningOptimization(Graph g) {
        graph = g;
        BackEdges.enable(graph);
        activatedGraphs.add(graph);
        NodeCollector c = new NodeCollector(worklist);
        graph.walkTopological(c);
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
                //.out.println(temp + " removed");
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
        System.out.println("COPY START");
        Node returnNode;
        Proj returnProj;


        System.out.println("LIST OF COPIED "+ listOfToBeCopied);
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



        /*Stack<Node> nodesToAdd = new Stack<>();
        Node returnNode = null;
        Proj returnProj = null;
        sourceReturn.getPreds().forEach(node -> nodesToAdd.add(node));
        Graph graph = targetBlock.getGraph();
        //addressGraph = sourceReturn.getGraph();
        //BackEdges.enable(addressGraph);

        ArrayList<Node> returns = new ArrayList();
        sourceReturn.getPreds().forEach(returns::add);
        returnProj = (Proj) sourceReturn.getPred(0);
        if (returns.size() < 3)
            returnNode = sourceReturn.getPred(1);
        else {
            System.out.println("More than one possible return!!");
        }

        ArrayList<Node> copied = new ArrayList<>();
        ArrayList<Node> tempCopied = new ArrayList<>();
        nodesToAdd.forEach(node -> System.out.println(node.getGraph()));
        nodesToAdd.stream().filter(node -> !(node instanceof Start)).forEach(node -> {
            node = node.copyInto(graph);
            node.setBlock(targetBlock);
            tempCopied.add(node);
        });
        nodesToAdd.removeAllElements();
        nodesToAdd.addAll(tempCopied);
        returnNode = tempCopied.get(1);
        returnProj = (Proj) tempCopied.get(0);
        nodesToAdd.forEach(node -> System.out.println(node.getGraph()));
        copied.addAll(nodesToAdd);
        while(!nodesToAdd.isEmpty()) {
            System.out.println(nodesToAdd);
            Node node = nodesToAdd.pop();
            ArrayList<Node> preds = new ArrayList<>();
            node.getPreds().forEach(preds::add);
            copied.addAll(preds);
            ArrayList<Node> tempC = new ArrayList<>();
            preds.stream().filter(node1 -> !(node1 instanceof Start) || !copied.contains(node1)).forEach(node1 -> {
                System.out.println("Copying "+  node1 + " into " + graph);
                node1 = node1.copyInto(graph);
                node1.setBlock(targetBlock);
                tempC.add(node1);
            });
            nodesToAdd.addAll(tempC);
            for (int i = 0; i < tempC.size(); i++) {
                node.setPred(i, tempC.get(i));
            }

            System.out.println(node + " in graph " + node.getGraph());
        }*/
        System.out.println("COPY END");
        return new Pair<>(returnNode, returnProj);
    }

    private void inline(Optional<Proj> resultNode, Node aboveMemoryNodeBeforeAddressCall, Proj belowMemoryNodeAfterAddressCall, ArrayList<Node> sourceParameters, ArrayDeque<Node> remoteParameters, Node remoteResultNode, Proj remoteLastMemoryNode, Proj remoteFirstMemoryNode ) {
        assert(sourceParameters.size() == remoteParameters.size());
        assert (resultNode != null && aboveMemoryNodeBeforeAddressCall != null && remoteFirstMemoryNode != null);

        System.out.println("resultNode " + resultNode + " aboveMemoryNodeBeforeAddressCall " + aboveMemoryNodeBeforeAddressCall + " belowMemoryNodeAfterAddressCall " + belowMemoryNodeAfterAddressCall +
                 " sourceParameters " + sourceParameters + " remoteParameters " + remoteParameters + " remoteResultNode " + remoteResultNode + " remoteLastMemoryNode " + remoteLastMemoryNode + " remoteFirstMemoryNode " + remoteFirstMemoryNode);



        resultNode.ifPresent(proj -> proj.setPred(remoteResultNode));
        belowMemoryNodeAfterAddressCall.setPred(remoteLastMemoryNode);
        remoteFirstMemoryNode.setPred(aboveMemoryNodeBeforeAddressCall);
        ArrayList<Node> parameterChildren = new ArrayList<>();
        remoteParameters.stream()
                .forEach(node -> BackEdges.getOuts(node).forEach(edge -> {
                    if (!parameterChildren.contains(edge.node))
                        parameterChildren.add(edge.node);
                }));
        System.out.println("REACHED " + remoteParameters + " remote " + sourceParameters);
        for (Node parameterChild : parameterChildren) {
            Iterator<Node> preds = parameterChild.getPreds().iterator();
            int i = 0;
            Node pred;
            while (preds.hasNext()) {
                pred = preds.next();
                if (remoteParameters.contains(pred))
                    parameterChild.setPred(i, sourceParameters.get(i));
                i++;
            }
        }
        System.out.println("INLINING DONE");

    }

    public void collectNodes() {


        findAllAddressCalls(worklist);
        for (Call callNode : callNodes) {
            System.out.println(callNode);
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

            callNode.getPreds().forEach(node -> {
                System.out.println(node);
                temp.add(node);
            });

            for (Node node : temp) {
                System.out.println(node);
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
                        //System.out.println(address.getEntity().getName());
                        //System.out.println(address.getEntity().getName().matches("(_System_out_(write|println|read|flush))|__builtin_alloc_function__"));
                        curTargetGraph = address.getEntity().getGraph();
                        if (!BackEdges.enabled(curTargetGraph)) {
                            BackEdges.enable(curTargetGraph);
                            activatedGraphs.add(curTargetGraph);
                        }
                        System.out.println(BackEdges.enabled(curTargetGraph));
                        //System.out.println(BackEdges.enabled(addressGraph));
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
            for (BackEdges.Edge edge : BackEdges.getOuts(callNode)) {
                Node node = edge.node;
                if (node instanceof Proj proj && proj.getMode().equals(Mode.getM()))
                    belowMemoryNodeAfterAddressCall = proj;
                else if (node instanceof Proj proj && proj.getMode().equals(Mode.getT()))
                    resultNode = projNodes.stream().filter(proj1 -> proj1.getPred().equals(proj)).findFirst();
            }


            System.out.println("START INLINING");

            inline(resultNode, aboveMemoryNodeBeforeAddressCall, belowMemoryNodeAfterAddressCall, sourceParameters, remoteParameters, remoteResultNode, remoteLastMemoryNode, remoteFirstMemoryNode);

        }
        System.out.println("DEACTIVATING");
        activatedGraphs.forEach(BackEdges::disable);
        activatedGraphs.forEach(graph1 -> System.out.println(graph1.toString() + " : " + BackEdges.enabled(graph1)));


        //ArrayDeque<Node> addresslist = new ArrayDeque<>(worklist.stream()
          //      .filter(((node -> (node instanceof Address) && !(((Address) node).getEntity().getName().matches("_System_out_(write|println|read|flush)")) && !(((Address) node).getEntity().getName().matches("__builtin_alloc_function__")) && node.getMode().equals(Mode.getP()))))
            //            .collect(Collectors.toList()));

        /*addresslist.forEach(node -> System.out.println(node));

        if (addresslist.isEmpty()) return;

        addressNode = (Address) addresslist.getFirst();

        Node callingAddressNode = StreamSupport.stream(BackEdges.getOuts(addressNode).spliterator(),  false).collect(Collectors.toList()).get(0).node;

        Iterator<Node> i = callingAddressNode.getPreds().iterator();
        while(i.hasNext()) {
            Node tempNode = i.next();
            if (tempNode.getMode().equals(Mode.getP()) || tempNode.equals(addressNode)) continue;
            if (tempNode.getMode().equals(Mode.getM())) {
                predsMemoryCall = StreamSupport.stream(tempNode.getPreds().spliterator(), false).collect(Collectors.toList()).get(0);
                continue;
            }
            parameters.add(tempNode);
        }

        Iterator<BackEdges.Edge> t = BackEdges.getOuts(callingAddressNode).iterator();
        while (t.hasNext()) {
            BackEdges.Edge temp = t.next();
            Node tempNode = temp.node;
            if (tempNode.getMode().equals(Mode.getM())) {
                succsMemoryCall = tempNode;
                callingProjIs = StreamSupport.stream(BackEdges.getOuts(succsMemoryCall).spliterator(), false).collect(Collectors.toList()).get(0).node;
                int count = 0;
                for (Node pred : callingProjIs.getPreds()) {
                    if (pred.getMode().equals(Mode.getIs())) isNode = new Pair<Node, Integer>(pred, count);
                }
            }

        }
        succsMemoryCall = StreamSupport.stream(BackEdges.getOuts(callingAddressNode).spliterator(), false).filter(edge -> edge.node.getMode().equals(Mode.getM())).findFirst().get().node;
        addressGraph = addressNode.getEntity().getGraph();

        succsMemoryCall.setPred(0, predsMemoryCall);

        ArrayDeque<Node> addressWorklist = new ArrayDeque<>();
        BackEdges.enable(addressGraph);
        NodeCollector adc = new NodeCollector(addressWorklist);
        addressGraph.walkTopological(adc);  //Maybe this is not needed

        //addressWorklist.forEach(node -> System.out.println(node));

        //System.out.println();
        //BackEdges.getOuts(addressGraph.getStart()).forEach(edge -> System.out.println(edge.node));

        ArrayList<Node> addressParameterNodes = new ArrayList<>();
        ArrayList<Node> oth = DFSGraph(addressWorklist.stream().filter(node -> node instanceof Start).findFirst().get(), addressParameterNodes);

        System.out.println("Parameter");
        addressParameterNodes.forEach(node -> System.out.println(node));
        System.out.println("Needed nodes");
        oth.forEach(node -> System.out.println(node));


        //for (int temp = 0; temp < oth.size(); temp++) {
          //  oth.set();
        //}#
        oth.set(1, oth.get(1).copyInto(graph));

        oth.get(1).setBlock(callingProjIs.getBlock());
        oth.get(1).setPred(0, parameters.getFirst());
        oth.get(1).setPred(1, parameters.getLast());
        System.out.println(oth.get(1));
        oth.get(1).getPreds().forEach(node -> System.out.println(node));
        callingProjIs.setPred(2, oth.get(oth.size() - 1));











        //GraphWanderer addressGraph = new GraphWanderer(dress.getEntity().getGraph());
        //if (addressGraph.getAllMatching(node -> node instanceof Proj).size() > 1) //TODO: If the method contains a bigger more complex method, find out if better to leave as method
          //  return;

        //ArrayDeque<Node> successors = graphWanderer.getSuccesors(dress);
        /*if (successors.size() != 1) {
            return; //TODO: This should not be the case, as a method may only be called by a "Call"-Node, I think
        }
        Node callNode = successors.getFirst();
        ArrayDeque<Node> callSuccesors = graphWanderer.getSuccesors(callNode);



        //Changing the predecessor of the Proj M Node to skip the method call, which should then be removed by GC
        System.out.println("CallSuccesors2");
        callSuccesors.forEach(node -> System.out.println(node));*/
        //callSuccesors.stream().filter(node -> node.getMode().equals(Mode.getM())).forEach(node -> node.setPred(0, nodePredPred.getFirst()));
        //BackEdges.disable(activatedGraphs.get(1));
        //BackEdges.disable(graph);
    }

    private ArrayDeque<Node> getPred(Node node) {
        return new ArrayDeque<Node>(StreamSupport.stream(node.getPreds().spliterator(), false).toList());
    }

    //TODO: store which nodes are first and last
    private ArrayList<Node> DFSGraph(Node start, ArrayDeque<Node> parameters) {
        ArrayList<Node> result = new ArrayList<>();
        Stack<Node> stack = new Stack<>();
        Node curr = start;

        do {
            Iterator<Node> i = curr.getPreds().iterator();
            i.forEachRemaining(node -> {
                //System.out.println(node + " preds node");
                //System.out.println(stack);
                boolean flag = true;
                if (node instanceof Proj proj && node.getMode().equals(Mode.getIs())) {
                    //System.out.println(proj.getDebugInfo());
                    //System.out.println("Found parameter");
                    flag = false;
                    parameters.add(node);
                }
                else if (node instanceof Start) {
                    flag = false;
                    //System.out.println(stack.peek() + " what is left in stack +  " + stack.size());
                    if (!stack.isEmpty() && !stack.contains(node)) {
                        stack.add(0, node);
                    }
                }
                else if (!result.contains(node)) result.add(node);
                if (flag && !stack.contains(node)) stack.add(node);
            });
            curr = stack.pop();
            //System.out.println(curr + " current Node");
        } while (!stack.isEmpty() && !(curr instanceof Return));

        System.out.println(result);
        System.out.println(parameters);
        return result;
    }


}
