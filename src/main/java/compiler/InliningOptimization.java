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

    private Pair<Node[], Node[]> copyIntoGraph(ArrayList<Node> listOfToBeCopied, Pair<Node[], Node[]> returns, Node callNode, boolean containsControlFlow, ArrayList<Node> jmpList) {

        if (containsControlFlow) {
            Iterator<Node> preds = callNode.getBlock().getPreds().iterator();
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
            binding_irgmod.collect_phiprojs_and_start_block_nodes(graph.ptr);
            binding_irgmod.part_block(callNode.ptr);
        }
        Block targetBlock = (Block) callNode.getBlock();

        Graph graph = targetBlock.getGraph();
        HashMap<Node, Node> copied = new HashMap<>();
        HashMap<Block, Block> copiedBlocks = new HashMap<>();
        ArrayList<Node> emptyBlocks = new ArrayList<>();
        listOfToBeCopied.forEach(node -> System.out.println(node));
        listOfToBeCopied.stream().filter(node -> node instanceof Block).forEach(node -> {
            //if (listOfToBeCopied.stream().filter(node1 -> !(node1 instanceof Block)).anyMatch(node1 -> node1.getBlock().equals(node))){
                System.out.println("Before " + node);
                Block temp = (Block) node.copyInto(graph);
                System.out.println("Copied block " + temp);
                copiedBlocks.put((Block) node, temp);
            //}
            //else emptyBlocks.add(node);
        });
        listOfToBeCopied.removeAll(copiedBlocks.keySet());
        listOfToBeCopied.removeAll(emptyBlocks);

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
            System.out.println(block + " block " + copiedBlocks.get(block));
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
        if(containsControlFlow) {
            binding_irgraph.ir_free_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
            binding_irgraph.ir_free_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
        }

        return new Pair<>(returnList, memoryList);
    }

    private void inline(Node resultNode, Node aboveMemoryNodeBeforeAddressCall, Proj belowMemoryNodeAfterAddressCall, ArrayList<Node> sourceParameters, ArrayList<Proj> remoteParameters, Node[] remoteResultNode, Node[] remoteLastMemoryNode, Proj remoteFirstMemoryNode, ArrayList<Node> callNode, ArrayList<Node> jmpList, Node endBlock, boolean containsControlFlow) {
        System.out.println("resultNode " + resultNode + " aboveMemoryNodeBeforeAddressCall " + aboveMemoryNodeBeforeAddressCall + " belowMemoryNodeAfterAddressCall " + belowMemoryNodeAfterAddressCall + " sourceParameters " + sourceParameters + " remoteParameters " +
                remoteParameters + " remoteResultNode " + remoteResultNode + " remoteLastMemoryNode " + remoteLastMemoryNode + " remoteFirstMemoryNode " + remoteFirstMemoryNode + " callNode " + callNode);

        Node memoryPhi = null;
        if (remoteLastMemoryNode.length > 1) {
            memoryPhi = graph.newPhi(endBlock, remoteLastMemoryNode, remoteLastMemoryNode[0].getMode());
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
                resultPhi = graph.newPhi(endBlock, remoteResultNode, remoteResultNode[0].getMode());
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

        if (containsControlFlow)
            FirmUtils.setPreds(endBlock, jmpList);
        /*for (int i = 0; i < jmpList.size(); i++) {
            endBlock.setPred(i, jmpList.get(i));
        }*/


        for (Proj parameterChild : remoteParameters) {
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
                        varCaller.setPred(i, sourceParameters.get(parameterChild.getNum()));
                    i++;
                }
            }

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
            ArrayList<Proj> remoteParameters = new ArrayList<>();
            Node[] remoteResultNode = null;
            Node[] remoteLastMemoryNode = null;
            Proj remoteFirstMemoryNode = null;
            Return targetReturn = null;
            Graph curTargetGraph = null;
            int curTargetGraphSize = Integer.MAX_VALUE;
            boolean breakFlag = false;
            ArrayList<Node> jmpList = new ArrayList<>();
            Node endBlock;
            boolean containsControlFlow = false;

            callNode.getPreds().forEach(node -> {
                temp.add(node);
            });

            aboveMemoryNodeBeforeAddressCall = callNode.getPred(0).getPred(0);


            Address address = (Address) callNode.getPred(1);
            curTargetGraph = address.getEntity().getGraph();
            if (!BackEdges.enabled(curTargetGraph)) {
                BackEdges.enable(curTargetGraph);
            }
            if (!curTargetGraph.equals(graph)) {
                InliningOptimization optimization = new InliningOptimization(curTargetGraph, optimizedGraphs);
                curTargetGraphSize = optimization.getSize();
                if (!optimizedGraphs.contains(curTargetGraph))
                    optimization.collectNodes();
            }
            if (!BackEdges.enabled(curTargetGraph))
                BackEdges.enable(curTargetGraph);
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
                    memoryArrayList.add(returns.getPred(0));
                    if (returns.getPredCount() > 1)
                        resultArrayList.add(returns.getPred(1));

                }
                Node[] resultList = new Node[resultArrayList.size()];
                resultList = resultArrayList.toArray(resultList);
                Node[] memoryList = new Node[memoryArrayList.size()];
                memoryList = memoryArrayList.toArray(memoryList);

                unCopied = new Pair<>(memoryArrayList.size() == resultArrayList.size() ? resultList : null, memoryList);



                containsControlFlow = containsControlFlow(toBeCopied);
                Pair<Node[], Node[]> copiedItems = copyIntoGraph(toBeCopied, unCopied, callNode, containsControlFlow, jmpList);
                remoteResultNode = copiedItems.first;
                remoteLastMemoryNode = copiedItems.second;
                Node tempNode = remoteLastMemoryNode[0];
                while (tempNode.getPred(0).getGraph().equals(remoteLastMemoryNode[0].getGraph()) && !(tempNode.getPred(0) instanceof Start)) {
                    tempNode = tempNode.getPred(0);
                }
                remoteFirstMemoryNode = (Proj) tempNode;

                for (int i = 2; i < callNode.getPredCount(); i++) {
                    if (callNode.getPred(i) instanceof Proj && callNode.getMode().equals(Mode.getP()) && callNode.getPred(i).getPred(0).getPred(0) instanceof Start)
                            continue;   //Check if "this" call
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
                endBlock = BackEdges.getOuts((resultNode == null) ? belowMemoryNodeAfterAddressCall : resultNode).iterator().next().node.getBlock();




                inline(resultNode, aboveMemoryNodeBeforeAddressCall, belowMemoryNodeAfterAddressCall, sourceParameters, remoteParameters, remoteResultNode, remoteLastMemoryNode, remoteFirstMemoryNode, belowCallNode, jmpList, endBlock, containsControlFlow);
                BackEdges.disable(curTargetGraph);
            }

        }
        BackEdges.disable(graph);

    }


    private ArrayList<Node> DFSGraph(Node end, ArrayList<Proj> parameters) {

        Node argNode = end.getGraph().getArgs();
        BackEdges.getOuts(argNode).forEach(edge -> {
            if (!(edge.node instanceof Anchor))
                parameters.add((Proj) edge.node);
        });


        ArrayDeque<Node> targetWorkList = new ArrayDeque<>();
        NodeCollector c = new NodeCollector(targetWorkList);
        end.getGraph().walkTopological(c);

        parameters.sort(Comparator.comparingInt(value -> value.getNr()));
        targetWorkList.removeAll(parameters);
        targetWorkList.remove(end.getGraph().getStart());
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
