package compiler;

import firm.*;
import firm.nodes.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InliningOptimization {

    record Pair<Node, Integer>(Node node, int location){}

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

    public InliningOptimization(Graph g) {
        graph = g;
        BackEdges.enable(graph);
        NodeCollector c = new NodeCollector(worklist);
        graph.walkTopological(c);
    }

    public void collectNodes() {


        ArrayDeque<Node> addresslist = new ArrayDeque<>(worklist.stream()
                .filter(((node -> (node instanceof Address) && !(((Address) node).getEntity().getName().matches("_System_out_(write|println|read|flush)")) && !(((Address) node).getEntity().getName().matches("__builtin_alloc_function__")) && node.getMode().equals(Mode.getP()))))
                        .collect(Collectors.toList()));

        addresslist.forEach(node -> System.out.println(node));

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

        System.out.println("------");
        System.out.println(predsMemoryCall);
        System.out.println(addressNode);
        System.out.println(addressGraph);
        System.out.println(succsMemoryCall);
        parameters.forEach(node -> System.out.println(node));
        System.out.println("------");

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
        BackEdges.disable(addressGraph);
        BackEdges.disable(graph);
    }

    private ArrayDeque<Node> getPred(Node node) {
        return new ArrayDeque<Node>(StreamSupport.stream(node.getPreds().spliterator(), false).toList());
    }

    //TODO: store which nodes are first and last
    private ArrayList<Node> DFSGraph(Node start, ArrayList<Node> parameters) {
        ArrayList<Node> result = new ArrayList<>();
        Node curr = start;
        Stack<Node> stack = new Stack<>();
        do {
            Iterator<BackEdges.Edge> i = BackEdges.getOuts(curr).iterator();
            i.forEachRemaining(edge -> {
                Node node = edge.node;
                boolean flag = true;
                if (node instanceof Proj proj && proj.getMode().equals(Mode.getT()));
                else if (node instanceof Proj && node.getMode().equals(Mode.getIs())) {
                    System.out.println("Found parameter");
                    parameters.add(node);
                }
                else if (node instanceof Return) {
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
