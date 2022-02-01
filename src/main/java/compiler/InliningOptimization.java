package compiler;

import compiler.ast.Parameter;
import compiler.utils.FirmUtils;
import firm.*;
import firm.bindings.binding_irgmod;
import firm.bindings.binding_irgraph;
import firm.nodes.*;

import java.nio.file.attribute.PosixFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class InliningOptimization {

    record Pair<X, Y>(X first, Y second){}

    private Graph graph;
    private ArrayDeque<Node> worklist = new ArrayDeque<>();

    private ArrayList<Graph> activatedGraphs = new ArrayList<>();
    private final int MAX_COPY_SIZE = 250;
    private Stack<Graph> optimizedGraphs;


    private int size = Integer.MAX_VALUE;


    private ArrayList<Call> callNodes = new ArrayList<>();
    private ArrayList<Proj> projNodes = new ArrayList<>();


    public InliningOptimization(Graph g, Stack<Graph> optimizedGraphs) {
        graph = g;
        this.optimizedGraphs = optimizedGraphs;
        this.optimizedGraphs.add(graph);
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
            if (temp.stream().anyMatch(node -> node instanceof Address address && address.getEntity().getName().matches("(_System_(out|in)_(write|println|read|flush))|__builtin_alloc_function__"))) {
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

    private Pair<Node[], Node[]> copyIntoGraph(ArrayList<Node> listOfToBeCopied, Pair<Node[], Node[]> returns, Node callNode, ArrayList<Node> jmpList) {

        if (callNode != null) {
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
            binding_irgmod.collect_phiprojs_and_start_block_nodes(graph.ptr);
            binding_irgmod.part_block(callNode.ptr);
        }
        Block targetBlock = (Block) callNode.getBlock();

        Graph graph = targetBlock.getGraph();
        HashMap<Node, Node> copied = new HashMap<>();
        HashMap<Block, Block> copiedBlocks = new HashMap<>();
        listOfToBeCopied.stream().filter(node -> node instanceof Block).forEach(node -> {
            Block temp = (Block) node.copyInto(graph);
            copiedBlocks.put((Block) node, temp);
        });
        listOfToBeCopied.removeAll(copiedBlocks.keySet());

        for (Node node : listOfToBeCopied) {
            Node temp = node.copyInto(graph);
            if (copiedBlocks.containsKey(node.getBlock()))
                temp.setBlock(copiedBlocks.get(node.getBlock()));
            else
                temp.setBlock(targetBlock);


            copied.put(node, temp);
        }

        Node[] set = new Node[copied.size()];
        copied.values().toArray(set);
        for (int i = 0; i < set.length; i++) {
            Iterator<Node> te = set[i].getPreds().iterator();
            Node next;
            int j = 0;
            while(te.hasNext()) {
                next = te.next();
                if (copied.containsKey(next))
                    set[i].setPred(j, copied.get(next));
                j++;
            }
        }
        ArrayList<Node> jmpBlocks = new ArrayList<>();
        if (!copiedBlocks.isEmpty())
            copiedBlocks.keySet().iterator().next().getGraph().getEndBlock().getPreds().forEach(node -> jmpBlocks.add(node.getBlock()));
        for (Block block : copiedBlocks.keySet()) {
            for (int j = 0; j < block.getPredCount(); j++) {
                copiedBlocks.get(block).setPred(j, copied.get(block.getPred(j)));
            }
            if (jmpBlocks.contains(block))
                jmpList.add(graph.newJmp(copiedBlocks.get(block)));
        }

        Node[] returnList = null;

        if (returns.first != null) {
            returnList = new Node[returns.first.length];
            for (int i = 0; i < returns.first.length; i++) {
                returnList[i] = copied.getOrDefault(returns.first[i], returns.first[i]);
            }
        }
        Node[] memoryList = new Node[returns.second.length];
        for (int i = 0; i < returns.second.length; i++) {
            memoryList[i] = copied.getOrDefault(returns.second[i], returns.second[i]);
        }

        return new Pair<>(returnList, memoryList);
    }

    private void inline(Node resultNode, Node aboveMemoryNodeBeforeAddressCall, Proj belowMemoryNodeAfterAddressCall, ArrayList<Node> sourceParameters, ArrayList<Node> remoteParameters, Node[] remoteResultNode, Node[] remoteLastMemoryNode, Proj remoteFirstMemoryNode, ArrayList<Node> callNode, ArrayList<Node> jmpList) {
        System.out.println("resultNode " + resultNode + " aboveMemoryNodeBeforeAddressCall " + aboveMemoryNodeBeforeAddressCall + " belowMemoryNodeAfterAddressCall " + belowMemoryNodeAfterAddressCall + " sourceParameters " + sourceParameters + " remoteParameters " +
                remoteParameters + " remoteResultNode " + remoteResultNode + " remoteLastMemoryNode " + remoteLastMemoryNode + " remoteFirstMemoryNode " + remoteFirstMemoryNode + " callNode " + callNode);

        Node memoryPhi = null;
        if (remoteLastMemoryNode.length > 1) {
            memoryPhi = graph.newPhi(belowMemoryNodeAfterAddressCall.getBlock(), remoteLastMemoryNode, remoteLastMemoryNode[0].getMode());
        }

        for (Node node : callNode) {
            Iterator<Node> predsIterator = node.getPreds().iterator();
            for (int i = 0; i < node.getPredCount(); i++) {
                Node temp = predsIterator.next();
                if  (temp.equals(belowMemoryNodeAfterAddressCall)) {
                    if (remoteLastMemoryNode.length > 1)
                        node.setPred(i, memoryPhi);
                    else node.setPred(i, remoteLastMemoryNode[0]);
                }
            }
        }

        if (resultNode instanceof Proj proj) {
            Node resultPhi = null;
            if (remoteResultNode.length > 1) {
                resultPhi = graph.newPhi(belowMemoryNodeAfterAddressCall.getBlock(), remoteResultNode, remoteResultNode[0].getMode());
            }
            Node finalResultPhi = resultPhi;
            BackEdges.getOuts(proj).forEach(edge -> {
                Node node = edge.node;
                Iterator<Node> resultUser = node.getPreds().iterator();
                int i = 0;
                while (resultUser.hasNext()){
                    Node node1 = resultUser.next();
                    if (node1.equals(proj))
                        if (remoteResultNode.length > 1)
                            node.setPred(i, finalResultPhi);
                        else node.setPred(i, remoteResultNode[0]);
                    i++;
                }
            });
        }
        remoteFirstMemoryNode.setPred(aboveMemoryNodeBeforeAddressCall);

        System.out.println(belowMemoryNodeAfterAddressCall.getBlock().getPredCount() + " preds");
        System.out.println(belowMemoryNodeAfterAddressCall.getBlock() + " block");
        System.out.println(jmpList + " jmp");
        FirmUtils.setPreds(belowMemoryNodeAfterAddressCall.getBlock(), jmpList);


        int j = 0;
        for (Node parameterChild : remoteParameters) {
            for (BackEdges.Edge out : BackEdges.getOuts(parameterChild)) {
                if (!out.node.getGraph().equals(graph))
                    continue;
                Node varCaller = out.node;
                Iterator<Node> preds = varCaller.getPreds().iterator();
                Node pred;
                int i = 0;
                while (preds.hasNext()) {
                    pred = preds.next();
                    if (pred.equals(parameterChild))
                        varCaller.setPred(i, sourceParameters.get(j));
                    i++;
                }
            }
            j++;

        }
    }

    public void collectNodes() {

        findAllAddressCalls(worklist);

        for (Call callNode : callNodes) {
            ArrayList<Node> temp = new ArrayList<>();
            Node resultNode = null; //can be empty
            Node aboveMemoryNodeBeforeAddressCall = null;
            Proj belowMemoryNodeAfterAddressCall = null;
            ArrayList<Node> sourceParameters = new ArrayList<>();
            ArrayList<Node> remoteParameters = new ArrayList<>();
            Node[] remoteResultNode = null;
            Node[] remoteLastMemoryNode = null;
            Proj remoteFirstMemoryNode = null;
            Return targetReturn = null;
            Graph curTargetGraph = null;
            int curTargetGraphSize = Integer.MAX_VALUE;
            boolean breakFlag = false;
            ArrayList<Node> jmpList = new ArrayList<>();

            callNode.getPreds().forEach(node -> {
                temp.add(node);
            });

            aboveMemoryNodeBeforeAddressCall = callNode.getPred(0).getPred(0);


            Address address = (Address) callNode.getPred(1);
            curTargetGraph = address.getEntity().getGraph();
            if (!BackEdges.enabled(curTargetGraph)) {
                BackEdges.enable(curTargetGraph);
                activatedGraphs.add(curTargetGraph);
            }
            if (!curTargetGraph.equals(graph)) {
                InliningOptimization optimization = new InliningOptimization(curTargetGraph, optimizedGraphs);
                curTargetGraphSize = optimization.getSize();
                if (!optimizedGraphs.contains(curTargetGraph))
                    optimization.collectNodes();
            }
            if (curTargetGraphSize > MAX_COPY_SIZE) {
                breakFlag = true;
            }

            if (curTargetGraph.equals(graph)) {
                breakFlag = true;
            }

            if (curTargetGraph.getEndBlock().getPredCount() > 1) {
                breakFlag = true;
            }
            if (!breakFlag) {
                targetReturn = (Return) curTargetGraph.getEndBlock().getPred(0);
                ArrayList<Node> toBeCopied = DFSGraph(targetReturn, remoteParameters);
                Pair<Node[], Node[]> unCopied; //Result, Memory
                ArrayList<Node> memoryArrayList = new ArrayList<>();
                ArrayList<Node> resultArrayList = new ArrayList<>();

                for (Node returns : curTargetGraph.getEndBlock().getPreds()) {
                    returns.getPreds().forEach(node -> System.out.println(node + " node preds"));
                    memoryArrayList.add(returns.getPred(0));
                    if (returns.getPredCount() > 1)
                        resultArrayList.add(returns.getPred(1));

                }
                Node[] resultList = new Node[resultArrayList.size()];
                resultList = resultArrayList.toArray(resultList);
                Node[] memoryList = new Node[memoryArrayList.size()];
                memoryList = memoryArrayList.toArray(memoryList);

                System.out.println(memoryArrayList.size() == resultArrayList.size());
                unCopied = new Pair<>(memoryArrayList.size() == resultArrayList.size() ? resultList : null, memoryList);




                Pair<Node[], Node[]> copiedItems = copyIntoGraph(toBeCopied, unCopied, containsControlFlow(toBeCopied) ? callNode : null, jmpList);
                remoteResultNode = copiedItems.first;
                remoteLastMemoryNode = copiedItems.second;
                Node tempNode = remoteLastMemoryNode[0];
                while (tempNode.getPred(0).getGraph().equals(remoteLastMemoryNode[0].getGraph()) && !(tempNode.getPred(0) instanceof Start)) {
                    tempNode = tempNode.getPred(0);
                }
                remoteFirstMemoryNode = (Proj) tempNode;

                for (int i = 2; i < callNode.getPredCount(); i++) {
                    sourceParameters.add(callNode.getPred(i));
                }
            }


            if (curTargetGraphSize <= MAX_COPY_SIZE && !breakFlag) {
                ArrayList<Node> belowCallNode = new ArrayList<>();
                for (BackEdges.Edge edge : BackEdges.getOuts(callNode)) {
                    Node node = edge.node;
                    if (node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
                        belowMemoryNodeAfterAddressCall = proj;
                        BackEdges.getOuts(proj).iterator().forEachRemaining(changingMemory -> belowCallNode.add(changingMemory.node));
                    } else if (node instanceof Proj proj && proj.getMode().equals(Mode.getT()) && remoteResultNode != null)
                        resultNode = BackEdges.getOuts(proj).iterator().next().node;
                }
                inline(resultNode, aboveMemoryNodeBeforeAddressCall, belowMemoryNodeAfterAddressCall, sourceParameters, remoteParameters, remoteResultNode, remoteLastMemoryNode, remoteFirstMemoryNode, belowCallNode, jmpList);
            }
        }

    }


    private ArrayList<Node> DFSGraph(Node end, ArrayList<Node> parameters) {

        Start startNode = end.getGraph().getStart();
        ArrayList<Node> startBack = new ArrayList<>();
        BackEdges.getOuts(startNode).forEach(edge -> startBack.add(edge.node));
        ArrayDeque<Node> targetWorkList = new ArrayDeque<>();
        NodeCollector c = new NodeCollector(targetWorkList);
        end.getGraph().walkTopological(c);
        startBack.stream().filter(node -> node instanceof Proj && node.getMode().equals(Mode.getT()) && targetWorkList.contains(node))
                .forEach(node -> {
                    BackEdges.getOuts(node).forEach(edge -> {
                        parameters.add(edge.node);
                    });
                    targetWorkList.remove(node);
                });



        parameters.sort(Comparator.comparingInt(value -> value.getNr()));
        targetWorkList.removeAll(parameters);
        targetWorkList.remove(startNode);
        targetWorkList.remove(end.getGraph().getEnd());
        targetWorkList.remove(end);
        targetWorkList.remove(end.getGraph().getEndBlock());
        targetWorkList.remove(end.getGraph().getStartBlock());

        ArrayList<Node> result = new ArrayList<>();
        result.addAll(targetWorkList);

        return result;
    }

    private boolean containsControlFlow(ArrayList<Node> list) {
        return list.stream().anyMatch(node -> {

            if (node instanceof Jmp || node instanceof Cond) return true;
            return false;
        });
    }



}
