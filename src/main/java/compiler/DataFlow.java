package compiler;

import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.types.Ty;
import compiler.utils.FirmUtils;
import firm.*;
import firm.nodes.*;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DataFlow {

    public static abstract sealed class ConstantValue permits Unknown, Constant, Variable {
        public ConstantValue sup(ConstantValue other) {
            return switch (this) {
                case Unknown ignored -> other;
                case Constant c ->
                        switch (other) {
                            case Unknown ignored -> c;
                            case Constant c2 -> c.value.compare(c2.value) == Relation.Equal ? c : Variable.value;
                            case Variable v -> v;
                        };
                case Variable v -> v;
            };
        }
    }

    public static final class Variable extends ConstantValue {
        public static final Variable value = new Variable();
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Variable;
        }
        @Override
        public String toString() {
            return "Variable / Top";
        }
    }

    public static final class Unknown extends ConstantValue {
        public static final Unknown value = new Unknown();
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Unknown;
        }
        @Override
        public String toString() {
            return "Unknown / Bot";
        }
    }

    public static final class Constant extends ConstantValue {
        public TargetValue value;
        public Constant(TargetValue value) {
            this.value = value;
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Constant v && value == v.value;
        }
        @Override
        public String toString() {
            return "Constant " + value.toString();
        }
    }

    public record ConstantFolder(
            Map<Node, ConstantValue> values,
            Map<Block, Node> controlFlowNode,
            Map<Node, ControlFlowEdge> controlFlowTarget,
            Set<ControlFlowEdge> isExecutable) implements MiniJavaNodeVisitor {

        private void partialEval(Function<List<ConstantValue>, ConstantValue> eval, Node parent, Node... children) {
            if (Arrays.stream(children).anyMatch(n -> values.get(n) instanceof Unknown)) {
                values.put(parent, Unknown.value);
                return;
            }
            List<ConstantValue> args = Arrays.stream(children).map(values::get).collect(Collectors.toList());
            values.put(parent, eval.apply(args));
        }

        private void evalAux(Function<List<TargetValue>, ConstantValue> eval, Node parent, Node... children) {
            Function<List<ConstantValue>, ConstantValue> f = args -> {
                if (args.stream().allMatch(a -> a instanceof Constant)) {
                    List<TargetValue> argValues = args.stream().map(a -> ((Constant) a).value).collect(Collectors.toList());
                    return eval.apply(argValues);
                }
                return Variable.value;
            };
            partialEval(f, parent, children);
        }

        private void eval(Function<List<TargetValue>, TargetValue> eval, Node parent, Node... children) {
            evalAux(args -> new Constant(eval.apply(args)), parent, children);
        }

        private void unaryEval(Function<TargetValue, TargetValue> eval, Node parent, Node child) {
            this.eval(args -> eval.apply(args.get(0)), parent, child);
        }

        private void biEval(BiFunction<TargetValue, TargetValue, TargetValue> eval, Node parent, Node child1, Node child2) {
            this.eval(args -> eval.apply(args.get(0), args.get(1)), parent, child1, child2);
        }

        private void biPartialEval(BiFunction<ConstantValue, ConstantValue, ConstantValue> eval, Node parent, Node child1, Node child2) {
            this.partialEval(args -> eval.apply(args.get(0), args.get(1)), parent, child1, child2);
        }

        private void multiplicativeEval(BiFunction<TargetValue, TargetValue, TargetValue> eval, Node parent, Node child1, Node child2) {
            BiFunction<ConstantValue, ConstantValue, ConstantValue> f = (a, b) -> {
                if (a instanceof Constant ac && ac.value.isNull()) {
                    return new Constant(new TargetValue(0, ac.value.getMode()));
                }
                if (b instanceof Constant bc && bc.value.isNull()) {
                    return new Constant(new TargetValue(0, bc.value.getMode()));
                }
                if (!(a instanceof Constant ac && b instanceof Constant bc)) {
                    return new Variable();
                }
                return new Constant(eval.apply(ac.value, bc.value));
            };
            biPartialEval(f, parent, child1, child2);
        }

        private void cmpEval(Cmp parent, Node child1, Node child2) {
            biEval((a, b) -> parent.getRelation().contains(a.compare(b)) ? TargetValue.getBTrue() : TargetValue.getBFalse(), parent, child1, child2);
        }

        private void forward(Node parent, Node child) {
            this.unaryEval(arg -> arg, parent, child);
        }

        // `block` ensures that this node will not get optimized away.
        private void block(Node n) {
            values.put(n, Variable.value);
        }

        @Override
        public void visit(Add add) {
            biEval(TargetValue::add, add, add.getLeft(), add.getRight());
        }

        @Override
        public void visit(Address address) {
            block(address);
        }

        @Override
        public void visit(Block block) {
            block(block);
        }

        @Override
        public void visit(Call call) {
            block(call);
        }

        @Override
        public void visit(Cmp cmp) {
            cmpEval(cmp, cmp.getLeft(), cmp.getRight());
        }

        @Override
        public void visit(Cond cond) {
            block(cond);
        }

        @Override
        public void visit(Const aConst) {
            values.put(aConst, new Constant(aConst.getTarval()));
        }

        @Override
        public void visit(Conv conv) {
            // This is a bit of a hack, but it allows us to
            // implement constant folding for sizes in the one case
            // where it matters (right before a Conv node) without
            // having to track sizes through the entire constant
            // folding.
            if (conv.getOp() instanceof Size s) {
                TargetValue sizeValue = new TargetValue(s.getType().getSize(), s.getMode());
                values.put(conv, new Constant(sizeValue.convertTo(conv.getMode())));
            } else {
                unaryEval(tv -> tv.convertTo(conv.getMode()), conv, conv.getOp());
            }
        }

        @Override
        public void visit(Div div) {
            multiplicativeEval(TargetValue::div, div, div.getLeft(), div.getRight());
        }

        @Override
        public void visit(End end) {
            block(end);
        }

        @Override
        public void visit(Eor eor) {
            biEval(TargetValue::eor, eor, eor.getLeft(), eor.getRight());
        }

        @Override
        public void visit(Jmp jmp) {
            block(jmp);
        }

        @Override
        public void visit(Load load) {
            block(load);
        }

        @Override
        public void visit(Member member) {
            block(member);
        }

        @Override
        public void visit(Minus minus) {
            unaryEval(TargetValue::neg, minus, minus.getOp());
        }

        @Override
        public void visit(Mod mod) {
            multiplicativeEval(TargetValue::mod, mod, mod.getLeft(), mod.getRight());
        }

        @Override
        public void visit(Mul mul) {
            multiplicativeEval(TargetValue::mul, mul, mul.getLeft(), mul.getRight());
        }

        @Override
        public void visit(Not not) {
            unaryEval(TargetValue::not, not, not.getOp());
        }

        @Override
        public void visit(And and) {
            // And nodes are created by arithmetic optimizations and shouldn't exist during this phase.
            block(and);
        }

        @Override
        public void visit(Phi phi) {
            ConstantValue result = Unknown.value;
            var block = (Block) phi.getBlock();
            for (int i = 0; i < phi.getPredCount(); i++) {
                if (isExecutable.contains(new ControlFlowEdge(block, i))) {
                    var pred = phi.getPred(i);
                    result = result.sup(values.get(pred));
                }
            }
            values.put(phi, result);
        }

        @Override
        public void visit(Proj proj) {
            if (proj.getMode().equals(Mode.getM())) {
                block(proj);
                return;
            }
            // At least for constant folding, Proj is meaningless.
            // The value of a proj node is always defined by a node preceeding it
            // (e.g. a division node, or a start node).
            forward(proj, proj.getPred());
        }

        @Override
        public void visit(Return aReturn) {
            block(aReturn);
        }

        @Override
        public void visit(Size size) {
            block(size);
        }

        @Override
        public void visit(Start start) {
            block(start);
        }

        @Override
        public void visit(Store store) {
            block(store);
        }

        @Override
        public void visit(firm.nodes.Unknown unknown) {
            // By default, nodes are marked with Unknown, so we don't need to insert it here.
        }
    }

    public record ControlFlowEdge(Block target, int predIdx){}
    public record ConstantPropagation(Map<Node, ConstantValue> values, Set<ControlFlowEdge> executableEdges, Map<Node, ControlFlowEdge> controlFlowTarget) {}
    public static ConstantPropagation analyzeConstantFolding(Graph g, Map<Block, List<Phi>> blockPhis) {
        BackEdges.enable(g);

        Map<Block, Node> controlFlowNode = new HashMap<>();
        Map<Node, ControlFlowEdge> controlFlowTarget = new HashMap<>();
        g.walkBlocks(block -> {
            for (int i = 0; i < block.getPredCount(); i++) {
                var cfNode = block.getPred(i);
                var predBlock = (Block) cfNode.getBlock();
                assert cfNode.getMode().equals(Mode.getX());

                if (cfNode instanceof Proj cfProj) {
                    controlFlowNode.put(predBlock, cfProj.getPred());
                } else {
                    controlFlowNode.put(predBlock, cfNode);
                }

                controlFlowTarget.put(cfNode, new ControlFlowEdge(block, i));
            }
        });

        ArrayDeque<Node> valueWorklist = new ArrayDeque<>(FirmUtils.blockContent(g.getStartBlock()));
        ArrayDeque<ControlFlowEdge> edgeWorklist = new ArrayDeque<>();
        var startBlockJmp = controlFlowNode.get(g.getStartBlock());
        if (startBlockJmp instanceof Jmp || startBlockJmp instanceof Return) {
            edgeWorklist.addLast(controlFlowTarget.get(startBlockJmp));
        }
        Set<ControlFlowEdge> isExecutable = new HashSet<>();

        Map<Node, ConstantValue> values = new HashMap<>();
        FirmUtils.blockContent(g.getStartBlock()).forEach(node -> values.put(node, Unknown.value));
        ConstantFolder f = new ConstantFolder(values, controlFlowNode, controlFlowTarget, isExecutable);
        while (!valueWorklist.isEmpty() || !edgeWorklist.isEmpty()) {

            while (!valueWorklist.isEmpty()) {
                Node n = valueWorklist.removeFirst();
                ConstantValue oldValue = values.get(n);
                if (oldValue == null) {
                    // This node wasn't picked up by our traversal at the start and is hence dead.
                    continue;
                }

                var nodeBlock = (Block) n.getBlock();
                var nodesInReacheableBlock = IntStream.range(0, nodeBlock.getPredCount()).anyMatch(idx -> isExecutable.contains(new ControlFlowEdge(nodeBlock, idx)))
                        || nodeBlock.equals(g.getStartBlock());

                if (nodesInReacheableBlock) {
                    n.accept(f);

                    if (n instanceof Cond cond) {
                        var condition = values.get(cond.getSelector());

                        var executableControlFlow = new ArrayList<Node>();
                        switch (condition) {
                            case Constant c && c.value.equals(TargetValue.getBTrue()) -> executableControlFlow.add(FirmUtils.getCondTrueProj(cond));
                            case Constant c -> {
                                assert c.value.equals(TargetValue.getBFalse());
                                executableControlFlow.add(FirmUtils.getCondFalseProj(cond));
                            }
                            case Variable ignored -> {
                                executableControlFlow.add(FirmUtils.getCondTrueProj(cond));
                                executableControlFlow.add(FirmUtils.getCondFalseProj(cond));
                            }
                            case Unknown ignored -> {}
                        }

                        executableControlFlow.stream().map(controlFlowTarget::get).forEach(edgeWorklist::addLast);
                    }
                }

                if (!oldValue.equals(values.get(n))) {
                    for (BackEdges.Edge e : BackEdges.getOuts(n)) {
                        if (!(e.node instanceof Block)) {
                            valueWorklist.addLast(e.node);
                        }
                    }
                }
            }

            while (!edgeWorklist.isEmpty()) {
                ControlFlowEdge edge = edgeWorklist.removeFirst();

                if (!isExecutable.contains(edge)) {
                    var hasBlockBecomeReacheable = IntStream.range(0, edge.target.getPredCount()).noneMatch(idx -> isExecutable.contains(new ControlFlowEdge(edge.target, idx)));
                    isExecutable.add(edge);

                    if (hasBlockBecomeReacheable) {
                        var blockContent = FirmUtils.blockContent(edge.target);
                        valueWorklist.addAll(blockContent);
                        blockContent.forEach(node -> values.put(node, Unknown.value));
                    } else {
                        valueWorklist.addAll(blockPhis.get(edge.target));
                    }

                    var controlFlow = controlFlowNode.get(edge.target);
                    if (controlFlow instanceof Jmp || controlFlow instanceof Return) {
                        edgeWorklist.add(controlFlowTarget.get(controlFlow));
                    }
                }
            }
        }
        BackEdges.disable(g);
        return new ConstantPropagation(values, isExecutable, controlFlowTarget);
    }

    private static boolean isMemNode(Node n) {
        return n instanceof Proj p && p.getMode().isValuesInMode(Mode.getM())
                || n instanceof Phi phi && phi.getMode().isValuesInMode(Mode.getM())
                || n instanceof Store || n instanceof Load || n instanceof Call
                || n instanceof Div || n instanceof Mod;
    }

    private static <T> Set<T> intersect(List<Set<T>> sets) {
        assert sets.size() > 0;
        Set<T> primary = new HashSet<>(sets.get(0));
        for (Set<T> set : sets) {
            primary.retainAll(set);
        }
        return primary;
    }

    public record LoadLoad(Load firstLoad, Load secondLoad) {}
    public record StoreLoad(Store store, Load load) {}

    public static List<LoadLoad> analyzeLoadLoad(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences) {
        AliasAnalysis aa = new AliasAnalysis(nodeAstTypes);
        ArrayDeque<Node> worklist = new ArrayDeque<>();
        NodeCollector.run(g).stream().filter(DataFlow::isMemNode).forEach(worklist::add);
        Set<Load> allLoads = worklist.stream()
                .filter(n -> n instanceof Load)
                .map(n -> (Load) n)
                .collect(Collectors.toSet());
        Map<Node, Set<Load>> availableLoads = worklist.stream()
                .collect(Collectors.toMap(n -> n, n -> new HashSet<>(allLoads)));
        availableLoads.put(g.getStart(), new HashSet<>());
        BiConsumer<Node, Node> forward = (from, to) ->
                availableLoads.put(to, new HashSet<>(availableLoads.get(from)));
        BackEdges.enable(g);
        while (!worklist.isEmpty()) {
            Node n = worklist.removeFirst();
            Set<Load> previousAvailableLoads = availableLoads.get(n);
            switch (n) {
                case Proj p ->
                        forward.accept(p.getPred(), p);
                case Div d ->
                        forward.accept(d.getMem(), d);
                case Mod m ->
                        forward.accept(m.getMem(), m);
                case Phi phi -> {
                    Set<Load> availableLoadsAfterPhi = intersect(FirmUtils.preds(phi).stream()
                            .map(availableLoads::get)
                            .collect(Collectors.toList()));
                    availableLoads.put(phi, availableLoadsAfterPhi);
                }
                case Store s -> {
                    Set<Load> availableLoadsAfterStore = availableLoads.get(s.getMem()).stream()
                            .filter(load -> aa.guaranteedNotAliased(load.getPtr(), s.getPtr()))
                            .collect(Collectors.toSet());
                    availableLoads.put(s, availableLoadsAfterStore);
                }
                case Load l -> {
                    Set<Load> previous = new HashSet<>(availableLoads.get(l.getMem()));
                    // A previous available load that wasn't killed yet is better than l if they have the same pointer.
                    if (previous.stream().noneMatch(prevLoad -> prevLoad.getPtr().equals(l.getPtr()))) {
                        previous.add(l);
                    }
                    availableLoads.put(l, previous);
                }
                case Call c -> {
                    if (methodReferences.get(c) instanceof DefinedMethod) {
                        // We don't know anything about our loads or stores after a method call.
                        availableLoads.put(c, new HashSet<>());
                    } else {
                        // This is an alloc or an internal call - these don't touch any memory locations.
                        forward.accept(c.getMem(), c);
                    }
                }
                case Start ignored -> { /* ignored */ }
                default -> throw new AssertionError("Ran into non-memory-node case on nodes that are only memory nodes");
            }
            if (!previousAvailableLoads.equals(availableLoads.get(n))) {
                List<Node> changed = FirmUtils.backEdgeTargets(n).stream()
                        .filter(availableLoads::containsKey)
                        .collect(Collectors.toList());
                worklist.addAll(changed);
            }
        }
        List<LoadLoad> loadLoadPairs = new ArrayList<>();
        for (var n : availableLoads.keySet()) {
            Set<Load> loads = availableLoads.get(n);
            if (!(n instanceof Load l)) {
                continue;
            }
            Optional<Load> maybeLoadLoad = loads.stream()
                    .filter(aLoad -> !l.equals(aLoad) && l.getPtr().equals(aLoad.getPtr()))
                    .findFirst();
            if (maybeLoadLoad.isPresent()) {
                loadLoadPairs.add(new LoadLoad(maybeLoadLoad.get(), l));
            }
        }
        BackEdges.disable(g);
        return loadLoadPairs;
    }

    public static List<StoreLoad> analyzeStoreLoad(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences) {
        AliasAnalysis aa = new AliasAnalysis(nodeAstTypes);
        ArrayDeque<Node> worklist = new ArrayDeque<>();
        NodeCollector.run(g).stream().filter(DataFlow::isMemNode).forEach(worklist::add);
        Set<Store> allStores = worklist.stream()
                .filter(n -> n instanceof Store)
                .map(n -> (Store) n)
                .collect(Collectors.toSet());
        Map<Node, Set<Store>> availableStores = worklist.stream()
                .collect(Collectors.toMap(n -> n, n -> new HashSet<>(allStores)));
        availableStores.put(g.getStart(), new HashSet<>());
        BiConsumer<Node, Node> forward = (from, to) ->
                availableStores.put(to, new HashSet<>(availableStores.get(from)));

        BackEdges.enable(g);
        while (!worklist.isEmpty()) {
            Node n = worklist.removeFirst();
            Set<Store> previousAvailableStores = availableStores.get(n);
            switch (n) {
                case Proj p ->
                        forward.accept(p.getPred(), p);
                case Div d ->
                        forward.accept(d.getMem(), d);
                case Mod m ->
                        forward.accept(m.getMem(), m);
                case Phi phi -> {
                    Set<Store> availableStoresAfterPhi = intersect(FirmUtils.preds(phi).stream()
                            .map(availableStores::get)
                            .collect(Collectors.toList()));
                    availableStores.put(phi, availableStoresAfterPhi);
                }
                case Store s -> {
                    Set<Store> availableStoresAfterStore = availableStores.get(s.getMem()).stream()
                            .filter(store -> aa.guaranteedNotAliased(store.getPtr(), s.getPtr()))
                            .collect(Collectors.toSet());
                    availableStoresAfterStore.add(s);
                    availableStores.put(s, availableStoresAfterStore);
                }
                case Load l ->
                        forward.accept(l.getMem(), l);
                case Call c -> {
                    if (methodReferences.get(c) instanceof DefinedMethod) {
                        // We don't know anything about our stores after a method call.
                        availableStores.put(c, new HashSet<>());
                    } else {
                        // This is an alloc or an internal call - these don't touch any memory locations.
                        forward.accept(c.getMem(), c);
                    }
                }
                case Start ignored -> { /* ignored */ }
                default -> throw new AssertionError("Ran into non-memory-node case on nodes that are only memory nodes");
            }
            if (!previousAvailableStores.equals(availableStores.get(n))) {
                List<Node> changed = FirmUtils.backEdgeTargets(n).stream()
                        .filter(availableStores::containsKey)
                        .collect(Collectors.toList());
                worklist.addAll(changed);
            }
        }
        List<StoreLoad> storeLoadPairs = new ArrayList<>();
        for (var n : availableStores.keySet()) {
            Set<Store> stores = availableStores.get(n);
            if (!(n instanceof Load l)) {
                continue;
            }
            Optional<Store> maybeStoreLoad = stores.stream()
                    .filter(aStore -> l.getPtr().equals(aStore.getPtr()))
                    .findFirst();
            if (maybeStoreLoad.isPresent()) {
                storeLoadPairs.add(new StoreLoad(maybeStoreLoad.get(), l));
            }
        }
        BackEdges.disable(g);
        return storeLoadPairs;
    }
}
