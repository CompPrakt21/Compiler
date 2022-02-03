package compiler;

import compiler.semantic.resolution.DefinedMethod;
import compiler.utils.FirmUtils;
import compiler.utils.GenericNodeWalker;
import firm.BackEdges;
import firm.Dump;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgmod;
import firm.bindings.binding_irgraph;
import firm.nodes.*;

import java.util.*;

public class Inlining {
    private final TranslationResult translation;
    private final FrontendResult frontend;

    private static final int MAX_GRAPH_SIZE = 1500;

    /**
     * Stores the graph size for a method as a bad heuristic for function complexity.
     */
    private final HashMap<DefinedMethod, Integer> functionComplexity;
    private final HashMap<DefinedMethod, List<Call>> calls;

    private final boolean dumpGraphs;

    public Inlining(FrontendResult frontend, TranslationResult translation, boolean dumpGraphs) {
        this.frontend = frontend;
        this.translation = translation;
        this.dumpGraphs = dumpGraphs;

        this.functionComplexity = new HashMap<>();
        this.calls = new HashMap<>();
    }

    private void traverseCallGraphForCycles(
            Map<DefinedMethod, Set<DefinedMethod>> callGraph,
            Stack<DefinedMethod> trace,
            Set<DefinedMethod> visited,
            DefinedMethod method,
            Set<DefinedMethod>isRecursive
    ) {
        var index = trace.indexOf(method);
        if (index != -1) {
            // We have found a cycle.
            for (int i = index; i < trace.size(); i++) {
                isRecursive.add(trace.get(i));
            }
        }

        if (visited.contains(method)) {
            return;
        }
        visited.add(method);

        trace.push(method);
        var calledMethods = callGraph.get(method);
        if (calledMethods != null) {
            for (var calledMethod : calledMethods) {
                traverseCallGraphForCycles(callGraph, trace, visited, calledMethod, isRecursive);
            }
        }
        trace.pop();
    }

    private void traverseIsReachable(Set<DefinedMethod> visited, DefinedMethod method) {
        if (visited.contains(method)) {
            return;
        }
        visited.add(method);

        for (var call : this.calls.get(method)) {
            var calledMethod = (DefinedMethod) this.translation.methodReferences().get(call);
            this.traverseIsReachable(visited, calledMethod);
        }
    }

    public void inline() {
        // transitive closure over the call graph for recursion detection purposes.
        Map<DefinedMethod, Set<DefinedMethod>> callGraph = new HashMap<>();

        // Collect information about each function.
        for (var pair : translation.methodGraphs().entrySet()) {
            var method = pair.getKey();
            var graph = pair.getValue();

            calls.put(method, new ArrayList<>());

            int[] counter = {0};
            GenericNodeWalker.walkNodes(graph, node -> {
                counter[0]++;

                if (node instanceof Call call) {
                    if (this.translation.methodReferences().get(call) instanceof DefinedMethod calledMethod) {
                        calls.get(method).add(call);
                        callGraph.putIfAbsent(method, new HashSet<>());
                        callGraph.get(method).add(calledMethod);
                    }
                }
            });

            functionComplexity.put(method, counter[0]);
        }

        var isRecursive = new HashSet<DefinedMethod>();
        this.traverseCallGraphForCycles(callGraph, new Stack<>(), new HashSet<>(), frontend.mainMethod(), isRecursive);

        var isInlined = new HashSet<DefinedMethod>();
        this.inlineRecursive(isRecursive, this.frontend.mainMethod(), isInlined);

        // Remove unreacheable functions from translation result.
        var reacheable = new HashSet<DefinedMethod>();
        this.traverseIsReachable(reacheable, frontend.mainMethod());
        var reacheableMap = new HashMap<DefinedMethod, Graph>();
        this.translation.methodGraphs().entrySet().stream().filter(pair -> reacheable.contains(pair.getKey())).forEach(pair -> reacheableMap.put(pair.getKey(), pair.getValue()));
        this.translation.methodGraphs().clear();
        this.translation.methodGraphs().putAll(reacheableMap);
    }

    private void inlineRecursive(HashSet<DefinedMethod> isRecursive, DefinedMethod method, HashSet<DefinedMethod> isInlined) {
        if (isInlined.contains(method)) {
            return;
        }
        isInlined.add(method);

        this.calls.get(method).forEach(call -> {
            var calleeMethod = (DefinedMethod) this.translation.methodReferences().get(call);
            this.inlineRecursive(isRecursive, calleeMethod, isInlined);
        });

        while (true) {
            if (this.functionComplexity.get(method) > MAX_GRAPH_SIZE) {
                return;
            }

            var selectedMethod = this.calls.get(method).stream().map(call -> {
                        var calleeMethod = (DefinedMethod) this.translation.methodReferences().get(call);
                        assert calleeMethod != null;
                        var calleeComplexity = this.functionComplexity.get(calleeMethod);

                        record Tuple<X, Y, Z>(X x, Y y, Z z) {
                        }
                        return new Tuple<>(call, calleeComplexity, calleeMethod);
                    })
                    .filter(tuple -> !isRecursive.contains(tuple.z)) // never recursive functions.
                    .min(Comparator.comparing(p -> p.y));

            if (selectedMethod.isPresent()) {
                var callToInline = selectedMethod.get().x;
                var inlinedComplexity = selectedMethod.get().y;

                var newInlinedCalls = this.inlineCall(callToInline);
                var newInlineableCalls = newInlinedCalls.stream().filter(call -> translation.methodReferences().get(call) instanceof DefinedMethod).toList();
                this.calls.get(method).remove(callToInline);
                this.calls.get(method).addAll(newInlineableCalls);

                var oldComplexity = this.functionComplexity.get(method);
                var newComplexity = oldComplexity + inlinedComplexity; // I now this isnt precise but I don't want to traverse the graph again.
                this.functionComplexity.put(method, newComplexity);
            } else {
                return;
            }
        }
    }

    /**
     * Inlines a call node.
     *
     * @param call The node to inline.
     * @return The list of new call nodes in the firm graph.
     */
    public List<Call> inlineCall(Call call) {
        assert this.translation.methodReferences().containsKey(call);
        var targetGraph = call.getGraph();

        var calleeGraph = this.translation.methodGraphs().get((DefinedMethod) this.translation.methodReferences().get(call));

        var endBlock = call.getBlock();
        binding_irgraph.ir_reserve_resources(targetGraph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
        binding_irgraph.ir_reserve_resources(targetGraph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);
        binding_irgmod.collect_phiprojs_and_start_block_nodes(targetGraph.ptr);
        binding_irgmod.part_block(call.ptr);
        var startBlock = call.getBlock();
        assert !endBlock.equals(startBlock);

        var calleeToTargetMap = new HashMap<Node, Node>();
        calleeToTargetMap.put(calleeGraph.getStartBlock(), startBlock);

        calleeToTargetMap.put(calleeGraph.getEndBlock(), endBlock);

        assert !calleeGraph.equals(targetGraph);
        BackEdges.disable(calleeGraph);
        BackEdges.enable(calleeGraph);
        BackEdges.disable(targetGraph);
        BackEdges.enable(targetGraph);

        // Connect function arguments
        Proj argProj = (Proj) calleeGraph.getArgs();
        Proj argMemProj = (Proj) calleeGraph.getInitialMem();

        for (var edge : BackEdges.getOuts(argProj)) {
            if (!(edge.node instanceof Proj calleeArg)) continue;
            var targetArg = call.getPred(calleeArg.getNum() + 2);
            calleeToTargetMap.put(calleeArg, targetArg);
        }
        calleeToTargetMap.put(argMemProj, call.getMem());

        // Copy nodes from callee into target
        var nodeCopier = new ShorterNodeCopier(calleeToTargetMap, targetGraph, this.translation);
        nodeCopier.copy(calleeGraph);

        // Finally connect return value and mem
        Optional<Proj> valueProj = Optional.empty();
        Proj memProj = null;
        for (var edge : BackEdges.getOuts(call)) {
            assert edge.node instanceof Proj;
            if (edge.node.getMode().equals(Mode.getM())) {
                memProj = (Proj) edge.node;
            } else {
                assert edge.node.getMode().equals(Mode.getT());
                valueProj = Optional.of((Proj) BackEdges.getOuts(edge.node).iterator().next().node);
            }
        }
        assert memProj != null;

        // Collect the correct returned memory, value and jmp nodes to connect to the end block.
        var calleeEndBlock = calleeGraph.getEndBlock();
        var memPhiPreds = new Node[calleeEndBlock.getPredCount()];
        var valuePhiPreds = new Node[calleeEndBlock.getPredCount()];
        var jmps = new Node[calleeEndBlock.getPredCount()];

        for (int i = 0; i < calleeEndBlock.getPredCount(); i++) {
            var calleeRet = (Return) calleeEndBlock.getPred(i);
            var predBlock = (Block) calleeRet.getBlock();

            var targetMem = calleeToTargetMap.get(calleeRet.getMem());
            memPhiPreds[i] = targetMem;

            if (calleeRet.getPredCount() > 1) {
                var targetValue = calleeToTargetMap.get(calleeRet.getPred(1));
                valuePhiPreds[i] = targetValue;
            }

            var targetPredBlock = calleeToTargetMap.get(predBlock);
            var replacementJmp = (Jmp) targetGraph.newJmp(targetPredBlock);
            jmps[i] = replacementJmp;
        }

        if (calleeEndBlock.getPredCount() == 1) {
            endBlock.setPred(0, jmps[0]);
            binding_irgmod.exchange(memProj.ptr, memPhiPreds[0].ptr);
            valueProj.ifPresent(proj -> binding_irgmod.exchange(proj.ptr, valuePhiPreds[0].ptr));
        } else {
            FirmUtils.setPreds(endBlock, Arrays.asList(jmps));
            var memPhi = targetGraph.newPhi(endBlock, memPhiPreds, Mode.getM());
            var valuePhi = valueProj.map(proj -> targetGraph.newPhi(endBlock, valuePhiPreds, proj.getMode()));
            binding_irgmod.exchange(memProj.ptr, memPhi.ptr);
            valueProj.ifPresent(proj -> binding_irgmod.exchange(proj.ptr, valuePhi.orElseThrow().ptr));
        }

        BackEdges.disable(calleeGraph);
        BackEdges.disable(targetGraph);

        binding_irgraph.ir_free_resources(targetGraph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_PHI_LIST.val);
        binding_irgraph.ir_free_resources(targetGraph.ptr, binding_irgraph.ir_resources_t.IR_RESOURCE_IRN_LINK.val);

        if (dumpGraphs) {
            Dump.dumpGraph(targetGraph, String.format("inline-%s", calleeGraph.getEntity().getName()));
        }

        return nodeCopier.insertedCallNodes;
    }

    private static class ShorterNodeCopier {
        private final Map<Node, Node> calleeToTargetMap;
        private final Graph targetGraph;
        private final TranslationResult translation;

        private final List<Call> insertedCallNodes;

        public ShorterNodeCopier(Map<Node, Node> calleeToTargetMap, Graph targetGraph, TranslationResult translation) {
            this.calleeToTargetMap = calleeToTargetMap;
            this.targetGraph = targetGraph;
            this.translation = translation;
            this.insertedCallNodes = new ArrayList<>();
        }

        public void copy(Graph calleeGraph) {
            GenericNodeWalker.walkNodes(calleeGraph, node -> {
                if (node instanceof Return) {
                    // Returns are converted to jumps afterwards.
                    return;
                } else if (node instanceof Block b && this.calleeToTargetMap.containsKey(b)) {
                    // This block is either the start or end block and already present in map
                    return;
                } else if (node instanceof Proj && node.getGraph().getArgs().equals(node)) {
                    return;
                } else if (node instanceof Proj proj && (node.getGraph().getArgs().equals(proj.getPred()) || node.getGraph().getInitialMem().equals(proj))) {
                    return;
                } else if (node instanceof End || node instanceof Start || node instanceof Anchor) {
                    return;
                }

                var newNode = targetGraph.copyNode(node);
                var associatedType = this.translation.nodeAstTypes().get(node);
                if (associatedType != null) {
                    this.translation.nodeAstTypes().put(newNode, associatedType);
                }

                if (node instanceof Call call) {
                    var newCall = (Call) newNode;
                    this.translation.methodReferences().put(newCall, this.translation.methodReferences().get(node));
                    insertedCallNodes.add(newCall);
                }

                this.calleeToTargetMap.put(node, newNode);
            });

            for (var pair : this.calleeToTargetMap.entrySet()) {
                var targetNode = pair.getValue();
                var calleeNode = pair.getKey();

                if (calleeNode.equals(calleeGraph.getEndBlock()) || calleeNode.equals(calleeGraph.getStartBlock())) {
                    continue;
                } else if (calleeNode instanceof Proj proj
                        && (proj.getPred().equals(calleeGraph.getArgs()) || proj.equals(calleeGraph.getInitialMem()))) {
                    continue;
                }

                assert targetNode.getPredCount() == calleeNode.getPredCount();

                if (!(calleeNode instanceof Block)) {
                    if (targetNode instanceof Const || targetNode instanceof Address) {
                        targetNode.setBlock(targetGraph.getStartBlock());
                    } else {
                        var targetBlock = this.calleeToTargetMap.get(calleeNode.getBlock());
                        assert targetBlock != null;
                        targetNode.setBlock(targetBlock);
                    }
                }

                for (int i = 0; i < calleeNode.getPredCount(); i++) {
                    var targetPred = this.calleeToTargetMap.get(calleeNode.getPred(i));
                    assert targetPred != null;
                    targetNode.setPred(i, targetPred);
                }
            }
        }
    }
}