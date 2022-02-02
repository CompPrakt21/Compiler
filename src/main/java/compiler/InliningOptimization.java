package compiler;

import compiler.semantic.resolution.MethodDefinition;
import compiler.types.Ty;
import compiler.utils.FirmUtils;
import firm.*;
import firm.bindings.binding_irgmod;
import firm.bindings.binding_irgraph;
import firm.nodes.*;

import java.util.*;

public class InliningOptimization {

    record Pair<X, Y>(X first, Y second){}

    private Graph graph;
    private ArrayDeque<Node> worklist = new ArrayDeque<>();

    private final int MAX_COPY_SIZE = 250;
    private Stack<Graph> optimizedGraphs;
    private Node originalBlock = null;
    Map<Call, MethodDefinition> methodReferences;
    Map<Node, Ty> nodeAstTypes;

    private int size = Integer.MAX_VALUE;


    private ArrayList<Call> callNodes = new ArrayList<>();
    private ArrayList<Proj> projNodes = new ArrayList<>();


    public InliningOptimization(Graph g, Stack<Graph> optimizedGraphs, boolean recursive, Map<Call, MethodDefinition> methodReferences, Map<Node, Ty> nodeAstTypes) {
        this.methodReferences = methodReferences;
        this.nodeAstTypes = nodeAstTypes;
        graph = g;
        this.optimizedGraphs = optimizedGraphs;
        this.optimizedGraphs.add(graph);
        if (!BackEdges.enabled(graph))
            BackEdges.enable(graph);
        NodeCollector c = new NodeCollector(worklist);
        graph.walkTopological(c);
        if (recursive && worklist.stream().anyMatch(node -> node instanceof Address)) {
            return;
        }
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

            originalBlock = callNode.getBlock();
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
            binding_irgraph.ir_reserve_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
            binding_irgmod.collect_phiprojs_and_start_block_nodes(graph.ptr);
            binding_irgmod.part_block(callNode.ptr);
        }
        Block targetBlock = (Block) callNode.getBlock();

        Graph graph = targetBlock.getGraph();
        ArrayList<Node> originalNode = new ArrayList<>();
        ArrayList<Node> copiedNode = new ArrayList<>();
        ArrayList<Block> originalBlock = new ArrayList<>();
        ArrayList<Block> copiedBlock = new ArrayList<>();
        //HashMap<Node, Node> copied = new HashMap<>();
        //HashMap<Block, Block> copiedBlocks = new HashMap<>();
        listOfToBeCopied.stream().filter(node -> node instanceof Block).forEach(node -> {
            //if (listOfToBeCopied.stream().filter(node1 -> !(node1 instanceof Block)).anyMatch(node1 -> node1.getBlock().equals(node))){
                Block temp = (Block) node.copyInto(graph);
                originalBlock.add((Block) node);
                copiedBlock.add(temp);
                //copiedBlocks.put((Block) node, temp);
            //}
            //else emptyBlocks.add(node);
        });
        listOfToBeCopied.removeAll(originalBlock);

        for (Node node : listOfToBeCopied) {
            Node temp = node.copyInto(graph);


            int exists = originalBlock.indexOf(node.getBlock());
            temp.setBlock((exists < 0) ? targetBlock : copiedBlock.get(exists));
            //temp.setBlock((originalBlock.contains(node.getBlock())) ? copiedBlock.get(originalBlock.indexOf(node.getBlock())) : targetBlock);

            //temp.setBlock(copiedBlocks.getOrDefault(node.getBlock(), targetBlock));

            originalNode.add(node);
            copiedNode.add(temp);
            //copied.put(node, temp);
        }

        //Node[] set = new Node[copied.size()];
        //copied.values().toArray(set);
        for (Node node : copiedNode) {
            Iterator<Node> te = node.getPreds().iterator();
            Node next;
            int j = 0;
            while (te.hasNext()) {
                next = te.next();
                if (originalNode.contains(next))
                    node.setPred(j, copiedNode.get(originalNode.indexOf(next)));
                j++;
            }
        }
        Map<Call, MethodDefinition> tempMethodReferences = new HashMap<>(this.methodReferences);
        Set<Call> calls = tempMethodReferences.keySet();
        for (Call call : calls) {
            int exists = originalNode.indexOf(call);
            if (exists >= 0) {
                MethodDefinition temp = tempMethodReferences.get(call);
                this.methodReferences.put((Call) copiedNode.get(exists), temp);
            }
        }

        Map<Node, Ty> tempNodeAstTypes = new HashMap<>(nodeAstTypes);
        Set<Node> nodes = tempNodeAstTypes.keySet();
        for (Node node : nodes) {
            int exists = originalNode.indexOf(node);
            if (exists >= 0) {
                Ty temp = tempNodeAstTypes.get(node);
                this.nodeAstTypes.put(copiedNode.get(exists), temp);
            }
        }



        ArrayList<Node> jmpBlocks = new ArrayList<>();
        if (!copiedBlock.isEmpty())
            originalBlock.get(0).getGraph().getEndBlock().getPreds().forEach(node -> jmpBlocks.add(node.getBlock()));
            //copiedBlocks.keySet().iterator().next().getGraph().getEndBlock().getPreds().forEach(node -> jmpBlocks.add(node.getBlock()));
        for (Block block : originalBlock) {
            for (int j = 0; j < block.getPredCount(); j++) {
                copiedBlock.get(originalBlock.indexOf(block)).setPred(j, copiedNode.get(originalNode.indexOf(block.getPred(j))));
                //copiedBlocks.get(block).setPred(j, copied.get(block.getPred(j)));
            }
            if (jmpBlocks.contains(block))
                jmpList.add(graph.newJmp(copiedBlock.get(originalBlock.indexOf(block))));
                //jmpList.add(graph.newJmp(copiedBlocks.get(block)));
        }

        Node[] returnList = null;

        if (returns.first != null) {
            returnList = new Node[returns.first.length];
            for (int i = 0; i < returns.first.length; i++) {
                int exists = originalNode.indexOf(returns.first[i]);
                returnList[i] = (exists < 0) ? returns.first[i] : copiedNode.get(exists);
                //returnList[i] = copied.getOrDefault(returns.first[i], returns.first[i]);
            }
        }
        Node[] memoryList = new Node[returns.second.length];
        for (int i = 0; i < returns.second.length; i++) {
            int exists = originalNode.indexOf(returns.second[i]);
            memoryList[i] = (exists < 0) ? returns.second[i] : copiedNode.get(exists);
            //memoryList[i] = copied.getOrDefault(returns.second[i], returns.second[i]);
        }
        if(containsControlFlow) {
            binding_irgraph.ir_free_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
            binding_irgraph.ir_free_resources(graph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
        }

        return new Pair<>(returnList, memoryList);
    }

    private void inline(Node resultNode, Node aboveMemoryNodeBeforeAddressCall, Proj belowCallNode, ArrayList<Node> sourceParameters, ArrayList<Proj> remoteParameters, Node[] remoteResultNode, Node[] remoteLastMemoryNode, Proj remoteFirstMemoryNode, ArrayList<Node> belowMemoryNode, ArrayList<Node> jmpList, Node endBlock, boolean containsControlFlow) {
        //System.out.println("resultNode " + resultNode + " aboveMemoryNodeBeforeAddressCall " + aboveMemoryNodeBeforeAddressCall + " belowMemoryNodeAfterAddressCall " + belowCallNode + " sourceParameters " + sourceParameters + " remoteParameters " +
          //      remoteParameters + " remoteResultNode " + Arrays.toString(remoteResultNode) + " remoteLastMemoryNode " + Arrays.toString(remoteLastMemoryNode) + " remoteFirstMemoryNode " + remoteFirstMemoryNode + " callNode " + belowMemoryNode +
            //    " jmpList " + jmpList + " endBlock " + endBlock );


        if (containsControlFlow){


            /*int j = 0;
            for (Node pred : endBlock.getPreds()) {
                //System.out.println((j < endBlock.getPredCount() && splitBlock != null && !splitBlock.contains(endBlock.getPred(j))) + " runRHOUGH " + j);
                if (splitBlock != null && !endBlock.equals(splitBlock.get(0).getBlock()) && j < endBlock.getPredCount() && !splitBlock.contains(pred)) {
                    jmpList.add(j, pred);
                }
                j++;
            }*/
            FirmUtils.setPreds(endBlock, jmpList);






        }

        Node memoryPhi = null;
        if (remoteLastMemoryNode.length > 1) {
            memoryPhi = graph.newPhi(endBlock, remoteLastMemoryNode, remoteLastMemoryNode[0].getMode());
        }

        for (Node node : belowMemoryNode) {
            Iterator<Node> predsIterator = node.getPreds().iterator();
            for (int i = 0; i < node.getPredCount(); i++) {
                Node temp = predsIterator.next();
                if (temp.equals(belowCallNode)) {
                    if (remoteLastMemoryNode.length > 1)
                        node.setPred(i, memoryPhi);
                    else node.setPred(i, remoteLastMemoryNode[0]);
                }

            }
            //if (projNode)
              //  belowCallNode.setPred(0, (remoteLastMemoryNode.length > 1) ? memoryPhi : remoteLastMemoryNode[0]);
        }

        Node resultPhi = null;
        if (resultNode instanceof Proj proj) {
            if (remoteResultNode.length > 1) {
                resultPhi = graph.newPhi(endBlock, remoteResultNode, remoteResultNode[0].getMode());
            }
            Node finalResultPhi = resultPhi;
            BackEdges.getOuts(proj).forEach(edge -> {
                Node node = edge.node;
                Iterator<Node> resultUser = node.getPreds().iterator();
                int i = 0;
                while (resultUser.hasNext()) {
                    Node node1 = resultUser.next();
                    if (node1.equals(proj))
                        if (remoteResultNode.length > 1)
                            node.setPred(i, finalResultPhi);
                        else node.setPred(i, remoteResultNode[0]);
                    i++;
                }
            });
        }

        if (aboveMemoryNodeBeforeAddressCall.getMode().equals(Mode.getM())) {
            if (aboveMemoryNodeBeforeAddressCall instanceof Phi) {
                List<BackEdges.Edge> backEdges = FirmUtils.backEdges(remoteFirstMemoryNode);
                backEdges.forEach(edge1 -> {
                    Node beforeRemote = edge1.node;
                    for (int i = 0; i < beforeRemote.getPredCount(); i++) {
                        if (beforeRemote.getPred(i).equals(remoteFirstMemoryNode))
                            beforeRemote.setPred(i, aboveMemoryNodeBeforeAddressCall);
                    }
                });
            }
            List<BackEdges.Edge> edges = FirmUtils.backEdges(aboveMemoryNodeBeforeAddressCall);
            boolean phiFound = false;
            for (BackEdges.Edge edge : edges) {
                Node temp = edge.node;
                if (temp.equals(aboveMemoryNodeBeforeAddressCall))
                    continue;
                if (temp instanceof Phi phi) {
                    List<BackEdges.Edge> backEdges = FirmUtils.backEdges(remoteFirstMemoryNode);
                    backEdges.forEach(edge1 -> {
                        Node beforeRemote = edge1.node;
                        for (int i = 0; i < beforeRemote.getPredCount(); i++) {
                            if (beforeRemote.getPred(i).equals(remoteFirstMemoryNode))
                                beforeRemote.setPred(i, phi);
                        }
                    });



                    //FirmUtils.setPreds(beforeRemote, nodePreds);

                    phiFound = true;
                }
            }
            if (!phiFound) {

                Node[] listForPhi = new Node[]{aboveMemoryNodeBeforeAddressCall};
                Node phi = graph.newPhi(aboveMemoryNodeBeforeAddressCall.getBlock(), listForPhi, listForPhi[0].getMode());
                remoteFirstMemoryNode.setPred(phi);
            }
        } else {
            remoteFirstMemoryNode.setPred(aboveMemoryNodeBeforeAddressCall);
            remoteFirstMemoryNode.setBlock(aboveMemoryNodeBeforeAddressCall.getBlock());
        }


        for (Proj parameterChild : remoteParameters) {
            for (BackEdges.Edge out : FirmUtils.backEdges(parameterChild)) {
                if (!out.node.getGraph().equals(graph))
                    continue;
                Node varCaller = out.node;
                Iterator<Node> preds = varCaller.getPreds().iterator();
                Node pred;
                int i = 0;
                if (!varCaller.equals(belowCallNode.getPred())) {
                    while (preds.hasNext()) {
                        pred = preds.next();
                        if (pred.equals(parameterChild)) {
                            varCaller.setPred(i, sourceParameters.get(parameterChild.getNum()));
                        }
                        i++;
                    }
                }
            }

        }
    }

    public void collectNodes() {

        findAllAddressCalls(worklist);

        for (Call callNode : callNodes) {
            originalBlock = null;
            ArrayList<Node> temp = new ArrayList<>();
            Node resultNode = null; //can be empty
            Node aboveMemoryNodeBeforeAddressCall = null;
            Proj belowCallNode = null;
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
            Node endBlock = null;
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
                boolean optimized = optimizedGraphs.contains(curTargetGraph);
                InliningOptimization optimization = new InliningOptimization(curTargetGraph, optimizedGraphs, true, methodReferences, nodeAstTypes);
                curTargetGraphSize = optimization.getSize();
                if (!optimized)
                    optimization.collectNodes();
            }
            if (!BackEdges.enabled(curTargetGraph))
                BackEdges.enable(curTargetGraph);
            if (!BackEdges.enabled(graph))
                BackEdges.enable(graph);
            if (curTargetGraphSize > MAX_COPY_SIZE) {
                breakFlag = true;
            }

            if (curTargetGraph.equals(graph)) {
                breakFlag = true;
            }

            if (!breakFlag) {
                ArrayList<Node> toBeCopied = DFSGraph(curTargetGraph, remoteParameters);
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

                unCopied = new Pair<>(resultList.length > 0 ? resultList : null, memoryList);



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
                    sourceParameters.add(callNode.getPred(i));
                }
            }


            if (curTargetGraphSize <= MAX_COPY_SIZE && !breakFlag) {
                ArrayList<Node> belowMemoryNode = new ArrayList<>();
                for (BackEdges.Edge edge : BackEdges.getOuts(callNode)) {
                    Node node = edge.node;
                    if (node instanceof Proj proj && proj.getMode().equals(Mode.getM())) {
                        belowCallNode = proj;
                        BackEdges.getOuts(proj).iterator().forEachRemaining(changingMemory -> belowMemoryNode.add(changingMemory.node));
                    } else if (node instanceof Proj proj && proj.getMode().equals(Mode.getT()) && remoteResultNode != null)
                        resultNode = BackEdges.getOuts(proj).iterator().next().node;
                }
                if (originalBlock == null) {
                    if (resultNode != null)
                        endBlock = BackEdges.getOuts(resultNode).iterator().next().node.getBlock();
                    if (endBlock == null || !endBlock.equals(callNode.getBlock()))
                        endBlock = BackEdges.getOuts(belowCallNode).iterator().next().node.getBlock();
                } else
                    endBlock = originalBlock;

                System.out.println("INLINING " + curTargetGraph + " into " + graph);
                inline(resultNode, aboveMemoryNodeBeforeAddressCall, belowCallNode, sourceParameters, remoteParameters, remoteResultNode, remoteLastMemoryNode, remoteFirstMemoryNode, belowMemoryNode, jmpList, endBlock, containsControlFlow);

            }
            BackEdges.disable(curTargetGraph);
        }
        BackEdges.disable(graph);

    }


    private ArrayList<Node> DFSGraph(Graph targetGraph, ArrayList<Proj> parameters) {

        Node argNode = targetGraph.getArgs();
        BackEdges.getOuts(argNode).forEach(edge -> {
            if (!(edge.node instanceof Anchor))
                parameters.add((Proj) edge.node);
        });


        ArrayDeque<Node> targetWorkList = new ArrayDeque<>();
        NodeCollector c = new NodeCollector(targetWorkList);
        targetGraph.walkTopological(c);

        targetWorkList.remove(argNode);
        targetWorkList.removeAll(parameters);
        targetWorkList.remove(targetGraph.getStart());
        targetWorkList.remove(targetGraph.getEnd());
        targetWorkList.removeAll(FirmUtils.toList(targetGraph.getEndBlock().getPreds()));
        targetWorkList.remove(targetGraph.getEndBlock());
        targetWorkList.remove(targetGraph.getStartBlock());


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
