package compiler;

import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.types.Ty;
import compiler.utils.FirmUtils;
import firm.*;
import firm.bindings.*;
import firm.nodes.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class Optimization {

    private final Graph g;
    // We're not keeping this one entirely up to date over the course of the optimization.
    // This is fine because so far we only need data about `Proj` nodes, which we don't create ourselves.
    private final Map<Node, Ty> nodeAstTypes;
    private final Map<Call, MethodDefinition> methodReferences;

    private Map<Block, List<Phi>> blockPhis;

    public Optimization(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences) {
        this.g = g;
        this.nodeAstTypes = nodeAstTypes;
        this.methodReferences = methodReferences;
    }

    private void updateBlockPhis() {
        ArrayDeque<Node> nodes = NodeCollector.run(g);
        blockPhis = new HashMap<>();
        for (Node n : nodes) {
            if (n instanceof Block b) {
                blockPhis.putIfAbsent(b, new ArrayList<>());
                continue;
            }
            if (!(n instanceof Phi p)) {
                continue;
            }
            Block b = (Block) n.getBlock();
            blockPhis.putIfAbsent(b, new ArrayList<>());
            blockPhis.get(b).add(p);
        }
    }

    private static void dumpIfFlag(boolean dumpGraphs, Graph g, String name) {
        if (dumpGraphs) {
            Dump.dumpGraph(g, name);
        }
    }

    public static void optimizeMinimal(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences, boolean dumpGraphs) {
        Optimization o = new Optimization(g, nodeAstTypes, methodReferences);
        o.constantFolding();
        dumpIfFlag(dumpGraphs,g, "after-const");
    }

    public static void optimizeAlmostFull(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences, boolean dumpGraphs) {
        Optimization o = new Optimization(g, nodeAstTypes, methodReferences);
        o.constantFolding();
        dumpIfFlag(dumpGraphs,g, "after-const");
        o.eliminateRedundantSideEffects();
        dumpIfFlag(dumpGraphs,g, "after-redundant-sideeffect");
        o.simplifyArithmeticExpressions();
        dumpIfFlag(dumpGraphs,g, "after-arithmetic");
        o.loopInvariantCodeMotion();
        dumpIfFlag(dumpGraphs,g, "after-loop-invariance");
        o.commonSubexpressionElimination();
        dumpIfFlag(dumpGraphs,g, "after-cse");
        o.eliminateRedundantPhis();
        dumpIfFlag(dumpGraphs,g, "after-redundant-phis");
        o.eliminateSingletonBlocks();
        dumpIfFlag(dumpGraphs,g, "after-singleton");
        o.eliminateTrivialConds();
        dumpIfFlag(dumpGraphs,g, "after-trivial-conds");
        o.inlineTrivialBlocks();
        dumpIfFlag(dumpGraphs,g, "after-inline-trivial-blocks");
        //o.testAliasingAnalysis();
        //o.testLoadStore();
        o.loadLoad();
        dumpIfFlag(dumpGraphs, g, "after-load-load");
        o.storeLoad();
        dumpIfFlag(dumpGraphs, g, "after-store-load");
        o.eliminateRedundantPhis();
        dumpIfFlag(dumpGraphs,g, "after-redundant-phis");
        o.eliminateUnusedAllocs();
        dumpIfFlag(dumpGraphs,g, "after-unused-allocs");
    }

    public static void optimizeFull(Graph g, Map<Node, Ty> nodeAstTypes, Map<Call, MethodDefinition> methodReferences, boolean dumpGraphs) {
        Optimization o = new Optimization(g, nodeAstTypes, methodReferences);
        o.constantFolding();
        dumpIfFlag(dumpGraphs,g, "after-const");
        o.eliminateRedundantSideEffects();
        dumpIfFlag(dumpGraphs,g, "after-redundant-sideeffect");
        o.simplifyArithmeticExpressions();
        dumpIfFlag(dumpGraphs,g, "after-arithmetic");
        o.simplifyDiv();
        dumpIfFlag(dumpGraphs,g, "after-divopt");
        o.loopInvariantCodeMotion();
        dumpIfFlag(dumpGraphs,g, "after-loop-invariance");
        o.commonSubexpressionElimination();
        dumpIfFlag(dumpGraphs,g, "after-cse");
        o.eliminateRedundantPhis();
        dumpIfFlag(dumpGraphs,g, "after-redundant-phis");
        o.eliminateSingletonBlocks();
        dumpIfFlag(dumpGraphs,g, "after-singleton");
        o.eliminateTrivialConds();
        dumpIfFlag(dumpGraphs,g, "after-trivial-conds");
        o.inlineTrivialBlocks();
        dumpIfFlag(dumpGraphs,g, "after-inline-trivial-blocks");
        //o.testAliasingAnalysis();
        //o.testLoadStore();
        o.loadLoad();
        dumpIfFlag(dumpGraphs, g, "after-load-load");
        o.storeLoad();
        dumpIfFlag(dumpGraphs, g, "after-store-load");
        o.eliminateRedundantPhis();
        dumpIfFlag(dumpGraphs,g, "after-redundant-phis");
        o.eliminateUnusedAllocs();
        dumpIfFlag(dumpGraphs,g, "after-unused-allocs");
    }

    public void constantFolding() {
        this.updateBlockPhis();
        var constantPropResult = DataFlow.analyzeConstantFolding(g, blockPhis);

        // Replace nodes determined to be constant with const nodes.
        var values = constantPropResult.values();
        record Change(Node node, int predIdx, Node folded) { }
        List<Change> changes = new ArrayList<>();
        for (Node n : values.keySet()) {
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                DataFlow.ConstantValue v = values.get(pred);
                if (v == null) {
                    continue;
                }
                Node folded = switch (v) {
                    case DataFlow.Unknown ignored -> g.newConst(0, pred.getMode());
                    case DataFlow.Constant c -> g.newConst(c.value);
                    case DataFlow.Variable ignored -> pred;
                };
                if (pred.getMode().equals(folded.getMode())) {
                    changes.add(new Change(n, i, folded));
                }
            }
        }
        // This warning is wrong, likely due to an IntelliJ bug.
        // changes is *not* empty.
        for (Change c : changes) {
            c.node.setPred(c.predIdx, c.folded);
        }

        // collect loop heads/tails so we can attach a keep alive edge if we replace the branch with a move
        // and thereby create an infinite loop.
        var loopHeadTails = new HashSet<>();
        binding_irdom.compute_doms(g.ptr);
        g.walkBlocks(head -> {
            for (var pred : head.getPreds()) {
                var tail = (Block) pred.getBlock();
                if (binding_irdom.block_dominates(head.ptr, tail.ptr) != 0) {
                    loopHeadTails.add(head);
                    loopHeadTails.add(tail);
                }
            }
        });

        // Remove dead control flow.
        BackEdges.enable(g);
        var executableControlFlow = constantPropResult.executableEdges();
        var controlFlowTarget = constantPropResult.controlFlowTarget();
        for (var liveEdge : executableControlFlow) {
            var pred = liveEdge.target().getPred(liveEdge.predIdx());

            if (pred instanceof Proj proj) {
                // The live edge comes from a branch.
                // If the other edge of this branch is not live we can convert it a simple jmp.
                var otherProj = FirmUtils.getOtherCondProj(proj);
                assert proj.getBlock().equals(otherProj.getBlock());

                var otherEdge = controlFlowTarget.get(otherProj);

                if (!executableControlFlow.contains(otherEdge)) {
                    var jmp = g.newJmp(proj.getBlock());
                    liveEdge.target().setPred(liveEdge.predIdx(), jmp);

                    if (loopHeadTails.contains(proj.getBlock())) {
                        g.keepAlive(proj.getBlock());
                        var phiLoop = blockPhis.get((Block) proj.getBlock()).stream().filter(phi -> phi.getMode().equals(Mode.getM())).findFirst();
                        phiLoop.ifPresent(g::keepAlive);
                    }
                }
            }
        }
        BackEdges.disable(g);

        for (var pair : this.blockPhis.entrySet()) {
            var block = pair.getKey();
            var phis = pair.getValue();

            var livePredIndices = IntStream.range(0, block.getPredCount()).filter(i -> executableControlFlow.contains(new DataFlow.ControlFlowEdge(block, i))).toArray();
            var newBlockPreds = Arrays.stream(livePredIndices)
                    .mapToObj(block::getPred)
                    .toList();
            FirmUtils.setPreds(block, newBlockPreds);

            for (var phi : phis) {
                var newPhiPreds = Arrays.stream(livePredIndices)
                        .mapToObj(phi::getPred)
                        .toList();
                FirmUtils.setPreds(phi, newPhiPreds);
            }
        }

        g.confirmProperties(binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
    }

    public void eliminateRedundantSideEffects() {
        // TODO: Const normalization?
        ArrayDeque<Node> nodes = NodeCollector.run(g);
        BackEdges.enable(g);
        for (Node n : nodes) {
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                if (!(pred.getMode().equals(Mode.getM()) && pred instanceof Proj proj)) {
                    continue;
                }
                Node divOrMod = proj.getPred();
                record D(Node left, Node right, Node mem) { }
                D d;
                switch (divOrMod) {
                    case Div div -> d = new D(div.getLeft(), div.getRight(), div.getMem());
                    case Mod mod -> d = new D(mod.getLeft(), mod.getRight(), mod.getMem());
                    default -> {
                        continue;
                    }
                }
                if (!(d.left instanceof Const && d.right instanceof Const divisor)) {
                    continue;
                }
                if (divisor.getTarval().asInt() == 0) {
                    continue;
                }
                // this is ok because we're walking the graph in topological order
                List<Node> outs = FirmUtils.backEdgeTargets(divOrMod);
                if (outs.size() > 1) {
                    continue;
                }
                n.setPred(i, d.mem);
            }
        }
        BackEdges.disable(g);
    }

    public void eliminateRedundantPhis() {
        ArrayDeque<Node> nodes = NodeCollector.run(g);
        for (Node n : nodes) {
            var invalidKeepAliveNodes = new ArrayList<Node>();
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                if (!(pred instanceof Phi p)) {
                    continue;
                }
                List<Node> in = FirmUtils.preds(p);
                if (in.size() == 0) {
                    invalidKeepAliveNodes.add(pred);
                    continue;
                }
                Node first = in.get(0);
                if (!in.stream().allMatch(first::equals)) {
                    continue;
                }
                if (n instanceof End) {
                    invalidKeepAliveNodes.add(pred);
                }else {
                    n.setPred(i, first);
                }
            }

            if (n instanceof End) {
                for (var pred : invalidKeepAliveNodes) {
                    binding_irnode.remove_End_keepalive(g.getEnd().ptr, pred.ptr);
                }
            }
        }

        BackEdges.disable(g);
        g.confirmProperties(binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
    }

    public void eliminateSingletonBlocks() {
        updateBlockPhis();
        BackEdges.enable(g);
        g.walkBlocksPostorder(block -> {
            List<Node> content = FirmUtils.blockContent(block);
            if (content.size() != 1) {
                return;
            }
            Node n = content.get(0);
            if (!(n instanceof Jmp)) {
                return;
            }
            List<Node> sources = FirmUtils.preds(block);
            if (sources.size() == 0) {
                return;
            }
            Node primarySource = sources.remove(0);
            List<BackEdges.Edge> targets = FirmUtils.backEdges(n);
            if (targets.size() != 1) {
                return;
            }
            BackEdges.Edge targetEdge = targets.get(0);
            Block target = (Block) targetEdge.node;
            List<Node> targetPreds = FirmUtils.preds(target);
            targetPreds.set(targetEdge.pos, primarySource);
            targetPreds.addAll(sources);
            FirmUtils.setPreds(target, targetPreds);
            List<Phi> phis = blockPhis.get(target);
            for (Phi p : phis) {
                List<Node> phiPreds = FirmUtils.preds(p);
                Node predToDuplicate = phiPreds.get(targetEdge.pos);
                for (int i = 0; i < sources.size(); i++) {
                    phiPreds.add(predToDuplicate);
                }
                FirmUtils.setPreds(p, phiPreds);
            }
            binding_irnode.remove_End_keepalive(g.getEnd().ptr, block.ptr);
        });
        BackEdges.disable(g);
        // We changed the control structure, so better be careful ...
        g.confirmProperties(binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
    }

    public void eliminateTrivialConds() {
        updateBlockPhis();
        BackEdges.enable(g);
        ArrayDeque<Node> nodes = NodeCollector.run(g);
        for (Node n : nodes) {
            if (!(n instanceof Cond c)) {
                continue;
            }
            List<Node> projs = FirmUtils.backEdgeTargets(c);
            if (projs.size() != 2) {
                continue;
            }
            Node maybeProjA = projs.get(0);
            Node maybeProjB = projs.get(1);
            if (!(maybeProjA instanceof Proj projA) || !(maybeProjB instanceof Proj projB)) {
                continue;
            }
            BackEdges.Edge blockEdgeA = FirmUtils.backEdges(projA).get(0);
            BackEdges.Edge blockEdgeB = FirmUtils.backEdges(projB).get(0);
            if (!blockEdgeA.node.equals(blockEdgeB.node)) {
                continue;
            }
            Block block = (Block) blockEdgeA.node;
            List<Phi> phis = blockPhis.get(block);
            boolean eligible = phis.stream().allMatch(phi ->
                    phi.getPred(blockEdgeA.pos).equals(phi.getPred(blockEdgeB.pos)));
            if (!eligible) {
                continue;
            }
            Node jmp = g.newJmp(c.getBlock());
            List<Node> blockPreds = FirmUtils.preds(block);
            blockPreds.set(blockEdgeA.pos, jmp);
            blockPreds.remove(blockEdgeB.pos);
            FirmUtils.setPreds(block, blockPreds);
            for (Phi p : phis) {
                List<Node> pPreds = FirmUtils.preds(p);
                pPreds.remove(blockEdgeB.pos);
                FirmUtils.setPreds(p, pPreds);
            }
        }
        BackEdges.disable(g);
        // We changed the control structure, so better be careful ...
        g.confirmProperties(binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
    }

    public void inlineTrivialBlocks() {
        BackEdges.enable(g);
        g.walkBlocksPostorder(block -> {
            List<Node> preds = FirmUtils.preds(block);
            if (preds.size() != 1) {
                return;
            }
            // This block should not have a Phi at this point, since it must be trivial.
            if (!(preds.get(0) instanceof Jmp j)) {
                return;
            }
            Block inlineTargetBlock = (Block) j.getBlock();
            List<Node> content = FirmUtils.blockContent(block);
            for (Node n : content) {
                n.setBlock(inlineTargetBlock);
            }
            FirmUtils.setPreds(block, new ArrayList<>());
            binding_irnode.remove_End_keepalive(g.getEnd().ptr, block.ptr);
        });
        BackEdges.disable(g);
        // We changed the control structure, so better be careful ...
        g.confirmProperties(binding_irgraph.ir_graph_properties_t.IR_GRAPH_PROPERTIES_NONE);
    }

    private static Optional<Node> commOp(Node left, Node right, BiFunction<Node, Node, Node> simplify) {
        return Optional.ofNullable(simplify.apply(left, right)).or(
                () -> Optional.ofNullable(simplify.apply(right, left)));
    }

    public void simplifyArithmeticExpressions() {
        // Ops: +, -, *, /, %, xor?, !
        // Simplifications at a glance:
        // --x = x
        // !!x = x
        // x + 0 = x
        // (-x) + (-y) = -(x + y)
        // x * 1 = x
        // x * (-y) = -(x * y)
        // x * +-2^k = optimized [TODO]
        // +-2^k * x = optimized [TODO]
        // Div and Mod are TODO
        // x / 1 = x
        // x / x = 1
        // x / (-y) = -(x / y)
        // (-x) / y = -(x / y)
        // x / +-2^k = optimized
        // (-x) % y = -(x % y)
        // x % (-y) = x % y
        // x % x = 0
        // x % 2^k = optimized
        // TODO: Negative literals
        ArrayDeque<Node> worklist = NodeCollector.run(g);
        BackEdges.enable(g);
        while (!worklist.isEmpty()) {
            Node n = worklist.removeFirst();
            Node b = n.getBlock();
            List<BackEdges.Edge> parents = FirmUtils.backEdges(n);
            Node nn;
            boolean nChanged = false;
            do {
                nn = null;
                switch (n) {
                    // --x = x
                    case Minus m1 && m1.getOp() instanceof Minus m2 -> nn = m2.getOp();
                    // !!x = x
                    case Not n1 && n1.getOp() instanceof Not n2 -> nn = n2.getOp();
                    case Add a -> {
                        Node left = a.getLeft();
                        Node right = a.getRight();
                        // x + 0 = x
                        Optional<Node> r1 = commOp(left, right, (l, r) -> {
                            if (r instanceof Const c && c.getTarval().isNull()) {
                                if (c.getMode().isReference()) {
                                    // x + NULL = NULL
                                    return c;
                                }
                                return l;
                            }
                            return null;
                        });
                        // (-x) + (-y) = -(x + y)
                        Supplier<Node> r2 = () -> {
                            if (left instanceof Minus l && right instanceof Minus r) {
                                return g.newMinus(b, g.newAdd(b, l.getOp(), r.getOp()));
                            }
                            return null;
                        };
                        nn = r1.orElseGet(r2);
                    }
                    case Mul m -> {
                        Node left = m.getLeft();
                        Node right = m.getRight();
                        // x * 1 = x
                        Supplier<Optional<Node>> r1 = () -> commOp(left, right, (l, r) ->
                                r instanceof Const c && c.getTarval().isOne() ?
                                        l : null);
                        // x * (-y) = -(x * y)
                        Supplier<Optional<Node>> r2 = () -> commOp(left, right, (l, r) -> {
                            if (r instanceof Minus min) {
                                Node mulNode = g.newMul(b, l, min.getOp());
                                worklist.addLast(mulNode);
                                return g.newMinus(b, mulNode);
                            }
                            return null;
                        });
                        nn = r1.get().or(r2).orElse(null);
                    }
                    default -> {}
                }
                // ALl the warnings related to nn and nChanged are wrong
                if (nn != null) {
                    n = nn;
                    nChanged = true;
                }
            } while (nn != null);
            if (nChanged) {
                for (BackEdges.Edge e : parents) {
                    e.node.setPred(e.pos, n);
                    worklist.addLast(e.node);
                }
            }
        }
        BackEdges.disable(g);
    }

    public void simplifyDiv() {
        ArrayDeque<Node> worklist = NodeCollector.run(g);
        BackEdges.enable(g);
        while (!worklist.isEmpty()) {
            Node origNode = worklist.removeFirst();
            Node block = origNode.getBlock();
            List<BackEdges.Edge> parents = FirmUtils.backEdges(origNode);

            Node newNode = null;
            Node priorMem = null;

            if (origNode instanceof Div div) {
                priorMem = div.getMem();
                Node left = div.getLeft();
                Node right = div.getRight();

                if (right instanceof Const) {
                    var tv = ((Const) right).getTarval();
                    var hbit = tv.highest_bit();

                    // Power of two
                    var powerOfTwo = hbit == tv.lowest_bit() && hbit != 0 && hbit != 31;
                    var tmpTv = tv.neg();
                    var tmpBit = tmpTv.highest_bit();
                    var negPowerOfTwo = tmpBit == tmpTv.lowest_bit() && tmpBit != 0 && tmpBit != 31;
                    if (powerOfTwo || negPowerOfTwo) {
                        if (negPowerOfTwo) {
                            hbit = tmpBit;
                        }
                        Node kMinusOne = g.newConst(hbit - 1, Mode.getBu());
                        Node firstShift = g.newShrs(block, left, kMinusOne);
                        Node thirtyTwoMinusK = g.newConst(32 - hbit, Mode.getBu());
                        Node secondShift = g.newShr(block, firstShift, thirtyTwoMinusK);
                        Node k = g.newConst(hbit, Mode.getBu());
                        Node add = g.newAdd(block, left, secondShift);
                        newNode = g.newShrs(block, add, k);

                        if (negPowerOfTwo) {
                            newNode = g.newMinus(block, newNode);
                        }
                    }
                }
            } else if (origNode instanceof Mod mod) {
                priorMem = mod.getMem();
                Node left = mod.getLeft();
                Node right = mod.getRight();

                if (right instanceof Const) {
                    var tv = ((Const) right).getTarval();
                    var hbit = tv.highest_bit();

                    // Power of two
                    if (hbit == tv.lowest_bit() && hbit != 0 && hbit != 31) {
                        Node kMinusOne = g.newConst(hbit - 1, Mode.getBu());
                        Node firstShift = g.newShrs(block, left, kMinusOne);
                        Node thirtyTwoMinusK = g.newConst(32 - hbit, Mode.getBu());
                        Node secondShift = g.newShr(block, firstShift, thirtyTwoMinusK);
                        Node add = g.newAdd(block, left, secondShift);
                        Node maskConst = g.newConst(tv.neg()); // -2**k
                        Node and = g.newAnd(block, add, maskConst);
                        Node minus = g.newMinus(block, and);
                        newNode = g.newAdd(block, left, minus);
                    }
                }
            }

            if (newNode != null) {
                for (BackEdges.Edge directEdge : parents) {
                    // Div/mod nodes are always accesed through projs
                    assert directEdge.node instanceof Proj;
                    Proj projNode = (Proj) directEdge.node;

                    if (projNode.getMode().equals(Mode.getM())) {
                        // Memory edge, this has to be redirected AROUND
                        // the replaced insn
                        for (BackEdges.Edge memDep : FirmUtils.backEdges(projNode)) {
                            memDep.node.setPred(memDep.pos, priorMem);
                        }
                    } else {
                        // Value edge, this should point to the new node
                        for (BackEdges.Edge valDep : FirmUtils.backEdges(projNode)) {
                            valDep.node.setPred(valDep.pos, newNode);
                        }
                    }
                }
            }

        }
        BackEdges.disable(g);
    }

    public record ExpressionNode(Node n) {
        private static HashMap<Node, Integer> hashCache = new HashMap<>();

        private static void cache(Node n, int hash) {
            hashCache.put(n, hash);
        }

        private static boolean equalAsNodes(Node a, Node b) {
            return a.getMode().equals(b.getMode());
        }

        @SafeVarargs
        private <T extends Node> boolean compare(T a, T b, Function<? super T, ? extends Node>... childGetters) {
            if (!equalAsNodes(a, b)) {
                return false;
            }
            for (var getter : childGetters) {
                Node aChild = getter.apply(a);
                Node bChild = getter.apply(b);
                if (!new ExpressionNode(aChild).equals(new ExpressionNode(bChild))) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ExpressionNode e)) {
                return false;
            }
            Node o = e.n;
            return switch (n) {
                case Add a1 -> o instanceof Add a2
                        && compare(a1, a2, Add::getLeft, Add::getRight);
                case Cmp c1 -> o instanceof Cmp c2 && c1.getRelation().equals(c2.getRelation())
                        && compare(c1, c2, Cmp::getLeft, Cmp::getRight);
                case Const c1 -> o instanceof Const c2 && equalAsNodes(c1, c2)
                        && c1.getTarval().compare(c2.getTarval()).equals(Relation.Equal);
                case Conv c1 -> o instanceof Conv c2
                        && compare(c1, c2, Conv::getOp);
                case Eor e1 -> o instanceof Eor e2
                        && compare(e1, e2, Eor::getLeft, Eor::getRight);
                case Member m1 -> o instanceof Member m2 && m1.getEntity().equals(m2.getEntity())
                        && compare(m1, m2, Member::getPtr);
                case Minus m1 -> o instanceof Minus m2
                        && compare(m1, m2, Minus::getOp);
                case Mul m1 -> o instanceof Mul m2
                        && compare(m1, m2, Mul::getLeft, Mul::getRight);
                case Not n1 -> o instanceof Not n2
                        && compare(n1, n2, Not::getOp);
                case Proj p1 -> o instanceof Proj p2 && p1.getNum() == p2.getNum()
                        && compare(p1, p2, Proj::getPred);
                case Size s1 -> o instanceof Size s2 && equalAsNodes(s1, s2)
                        && s1.getType().equals(s2.getType());
                default -> n.equals(o);
            };
        }

        private int hashAsNode() {
            return n.getMode().hashCode();
        }

        private int hash(Node... children) {
            return Objects.hash(hashAsNode(), Arrays.hashCode(Arrays.stream(children).map(ExpressionNode::new).toArray()));
        }

        @Override
        public int hashCode() {
            if (hashCache.containsKey(n)) {
                return hashCache.get(n);
            }
            int hash = switch (n) {
                case Add a    -> hash(a.getLeft(), a.getRight());
                case Cmp c    -> Objects.hash(c.getRelation(), hash(c.getLeft(), c.getRight()));
                case Const c  -> Objects.hash(c.getTarval().asInt(), hash());
                case Conv c   -> hash(c.getOp());
                case Eor e    -> hash(e.getLeft(), e.getRight());
                case Minus m  -> hash(m.getOp());
                case Member m -> Objects.hash(m.getEntity(), hash(m.getPtr()));
                case Mul m    -> hash(m.getLeft(), m.getRight());
                case Not n    -> hash(n.getOp());
                case Proj p   -> Objects.hash(p.getNum(), hash(p.getPred()));
                case Size s   -> Objects.hash(s.getType(), hash());
                default       -> n.hashCode();
            };
            cache(n, hash);
            return hash;
        }
    }

    public void commonSubexpressionElimination() {
        ExpressionNode.hashCache.clear();
        binding_irdom.compute_doms(g.ptr);
        BackEdges.enable(g);
        ArrayDeque<Node> nodes = NodeCollector.run(g);
        HashMap<ExpressionNode, List<Node>> exprNodes = new HashMap<>();
        for (Node n : nodes) {
            ExpressionNode e = new ExpressionNode(n);
            if (!exprNodes.containsKey(e)) {
                List<Node> syntacticallyEqualNodes = new ArrayList<>();
                syntacticallyEqualNodes.add(e.n);
                exprNodes.put(e, syntacticallyEqualNodes);
                continue;
            }
            List<Node> syntacticallyEqualNodes = exprNodes.get(e);
            Optional<Node> maybeDominator = syntacticallyEqualNodes.stream()
                    .filter(equalNode -> binding_irdom.block_dominates(equalNode.getBlock().ptr, n.getBlock().ptr) != 0)
                    .findFirst();
            // n is always only dominated by the block of at max a single node syntacticallyEqualNodes.
            // There cannot be more dominators; a second dominator of the block of n will never get added to the list
            // because those two dominators would necessarily dominate one another, in which case we ensure that
            // only one of them remains.
            if (maybeDominator.isPresent()) {
                Node dominator = maybeDominator.get();
                List<BackEdges.Edge> edges = FirmUtils.backEdges(n);
                for (var edge : edges) {
                    edge.node.setPred(edge.pos, dominator);
                }
                continue;
            }
            // syntacticallyEqualNodes contains no node in a block that dominates the block of n.
            // But maybe the block of n dominates a block of a node in syntacticallyEqualNodes!
            // In this case, n is a better candidate for CSE than that other node.
            // It also means that we can reuse n for the node in syntacticallyEqualNodes.
            boolean foundDominated = false;
            for (int i = 0; i < syntacticallyEqualNodes.size(); i++) {
                Node dominated = syntacticallyEqualNodes.get(i);
                if (binding_irdom.block_dominates(n.getBlock().ptr, dominated.getBlock().ptr) == 0) {
                    continue;
                }
                foundDominated = true;
                List<BackEdges.Edge> edges = FirmUtils.backEdges(dominated);
                for (var edge : edges) {
                    edge.node.setPred(edge.pos, n);
                }
                syntacticallyEqualNodes.set(i, n);
                // There cannot be other dominated blocks because the dominated blocks
                // would also dominate one another in some direction and we ensure that
                // the node with a dominated block is removed.
                break;
            }
            if (!foundDominated) {
                // This is an entirely new node in a new block that cannot be related to the blocks
                // of other nodes in syntacticallyEqualNodes.
                syntacticallyEqualNodes.add(n);
            }
            exprNodes.put(e, syntacticallyEqualNodes);
        }
        BackEdges.disable(g);
    }

    public void testAliasingAnalysis() {
        List<Node> loadStoreNodes = NodeCollector.run(g).stream()
                .filter(n -> n instanceof Load || n instanceof Store)
                .collect(Collectors.toList());
        AliasAnalysis aa = new AliasAnalysis(nodeAstTypes);
        for (Node a : loadStoreNodes) {
            Node aPtr = a instanceof Load l ? l.getPtr() : ((Store) a).getPtr();
            for (Node b : loadStoreNodes) {
                if (a.equals(b)) {
                    continue;
                }
                Node bPtr = b instanceof Load l ? l.getPtr() : ((Store) b).getPtr();
                boolean notAliased = aa.guaranteedNotAliased(aPtr, bPtr);
                if (notAliased) {
                    System.out.println("Not aliased: " + aPtr + " and " + bPtr);
                } else {
                    System.out.println("Possibly aliased: " + aPtr + " and " + bPtr);
                }
            }
        }
    }

    public void testLoadStore() {
        List<DataFlow.LoadLoad> r1 = DataFlow.analyzeLoadLoad(g, nodeAstTypes, methodReferences);
        for (DataFlow.LoadLoad ll : r1) {
            System.out.println(ll.firstLoad() + "; " + ll.secondLoad());
        }
        List<DataFlow.StoreLoad> r2 = DataFlow.analyzeStoreLoad(g, nodeAstTypes, methodReferences);
        for (DataFlow.StoreLoad sl : r2) {
            System.out.println(sl.store() + "; " + sl.load());
        }
    }

    public void loadLoad() {
        binding_irdom.compute_doms(g.ptr);
        List<DataFlow.LoadLoad> r = DataFlow.analyzeLoadLoad(g, nodeAstTypes, methodReferences);
        BackEdges.enable(g);
        for (DataFlow.LoadLoad ll : r) {
            Load dominator = ll.firstLoad();
            Load dominated = ll.secondLoad();
            for (BackEdges.Edge e : FirmUtils.backEdges(dominated)) {
                if (e.node.getMode().isValuesInMode(Mode.getM())) {
                    // The dominated Load and its associated Proj get removed on the memory path
                    if (!(e.node instanceof Proj p)) {
                        // This shouldn't happen, but let's be careful.
                        continue;
                    }
                    for (BackEdges.Edge e2 : FirmUtils.backEdges(p)) {
                        e2.node.setPred(e2.pos, dominated.getMem());
                    }
                } else {
                    // The value of the dominated Load is taken from the Proj node of the dominator.
                    Optional<Proj> maybeProj = FirmUtils.backEdgeTargets(dominator).stream()
                            .filter(n -> n instanceof Proj p && !p.getMode().isValuesInMode(Mode.getM()))
                            .map(n -> (Proj) n)
                            .findFirst();
                    if (maybeProj.isEmpty()) {
                        maybeProj = Optional.of((Proj) g.newProj(dominator, dominator.getLoadMode(), 1));
                    }
                    if (!(e.node instanceof Proj p)) {
                        // This shouldn't happen, but let's be careful.
                        continue;
                    }
                    for (BackEdges.Edge e2 : FirmUtils.backEdges(p)) {
                        e2.node.setPred(e2.pos, maybeProj.get());
                    }
                }
            }
        }
        BackEdges.disable(g);
    }

    public void storeLoad() {
        List<DataFlow.StoreLoad> r = DataFlow.analyzeStoreLoad(g, nodeAstTypes, methodReferences);
        BackEdges.enable(g);
        for (DataFlow.StoreLoad sl : r) {
            Store dominator = sl.store();
            Load dominated = sl.load();
            for (Node n : FirmUtils.backEdgeTargets(dominated)) {
                if (n.getMode().isValuesInMode(Mode.getM())) {
                    // The dominated Load and its associated Proj get removed on the memory path
                    if (!(n instanceof Proj p)) {
                        // This shouldn't happen, but let's be careful.
                        continue;
                    }
                    for (BackEdges.Edge e2 : FirmUtils.backEdges(p)) {
                        e2.node.setPred(e2.pos, dominated.getMem());
                    }
                } else {
                    // The value of the dominated Load is taken from the dominator and the associated
                    // Proj is removed.
                    if (!(n instanceof Proj p)) {
                        // This shouldn't happen, but let's be careful.
                        continue;
                    }
                    for (BackEdges.Edge e : FirmUtils.backEdges(p)) {
                        e.node.setPred(e.pos, dominator.getValue());
                    }
                }
            }
        }
        BackEdges.disable(g);
    }

    private void loopInvariantCodeMotionWalker(
            Node node,
            Set<Node> visited,
            Function<Load, Boolean> isUnaliased,
            Set<Block> loopBlocks,
            Block loopHead,
            Map<Node, Integer> movableNodes,
            Set<Load> movableLoads
    ) {
        // already visited node or the node is outside the loop.
        if (visited.contains(node) || !loopBlocks.contains((Block) node.getBlock())) {
            return;
        }
        visited.add(node);

        // Try moving predecessors first
        for (var pred : node.getPreds()) {
            loopInvariantCodeMotionWalker(pred, visited, isUnaliased, loopBlocks, loopHead, movableNodes, movableLoads);
        }

        // These nodes better not be moved.
        if (node instanceof Block
                || node instanceof End
                || node instanceof Start
                || node instanceof Jmp
                || node instanceof Cond
                || node instanceof Return
                || node instanceof Phi
        ) return;

        // The node can be moved before the loop if all predecessor are in a block which strictly dominates the loop head or
        // the predecessor can be moved outside the loop.
        Function<Node, Boolean> nodeMovable = n -> StreamSupport.stream(n.getPreds().spliterator(), false).allMatch(
                pred -> binding_irdom.block_dominates(pred.getBlock().ptr, loopHead.ptr) != 0 && !loopHead.equals(pred.getBlock())
                        || movableNodes.containsKey(pred)
        );

        boolean canBeMoved;
        if (node instanceof Load load) {
            canBeMoved = nodeMovable.apply(load.getPtr()) && isUnaliased.apply(load);
        } else {
            canBeMoved = nodeMovable.apply(node);
        }

        if (canBeMoved) {
            // We assign each movable node a weight to determine whether it should actually be moved before the loop.
            // We don't want to move cheap operations since every move increases register pressure.
            var predWeights = StreamSupport.stream(node.getPreds().spliterator(), false).map(pred -> {
                var c = movableNodes.get(pred);
                if (c == null) {
                    c = 0;
                }
                return c;
            }).max(Comparator.naturalOrder()).orElse(0);

            // We weight certain more or less depending how useful they are to be moved.
            var weight = predWeights + switch (node) {
                case Mul ignored -> 2;   // Comparatively expensive operation
                case Minus ignored -> 0; // almost always folded into a sub instruction
                case Conv ignored -> 0;
                case Cmp ignored -> 0;   // It doesn't matter if cmp nodes are moved, when generating the branch
                // which is still inside the loop, it will get pulled down again.
                case Load ignored -> 100;
                default -> 1;
            };

            if (node instanceof Load load) {
                movableLoads.add(load);
            } else {
                movableNodes.put(node, weight);
            }
        }
    }

    private void moveLoadOutsideLoop(Load load, Phi memPhiLoop, int memPhiIdx, Block target, Map<Node, Integer> movable) {
        if (movable.containsKey(load.getPtr())) {
            movable.remove(load.getPtr());
            this.moveNodeOutsideLoop(load.getPtr(), target, movable);
        }

        // Insert before phi loop in target. There is no need to remove the load.
        // The subsequent load-load optimization will remove the loads within the loop.
        var newLoad = g.newLoad(target, memPhiLoop.getPred(memPhiIdx), load.getPtr(), load.getLoadMode(), load.getType(), binding_ircons.ir_cons_flags.cons_none);
        var newProj = g.newProj(newLoad, Mode.getM(), 0);
        memPhiLoop.setPred(memPhiIdx, newProj);
    }

    private void moveNodeOutsideLoop(Node n, Block target, Map<Node, Integer> movable) {
        for (var pred : n.getPreds()) {
            if (movable.containsKey(pred)) {
                movable.remove(pred);
                this.moveNodeOutsideLoop(pred, target, movable);
            }
        }
        n.setBlock(target);
    }

    public void loopInvariantCodeMotion() {
        BackEdges.enable(g);
        binding_irdom.compute_doms(g.ptr);
        binding_irdom.compute_postdoms(g.ptr);

        record Loop(Block head, Block tail) {
        }
        List<Loop> loops = new ArrayList<>();

        // Find all loops in the graph
        g.walkBlocks(head -> {
            for (var pred : head.getPreds()) {
                var tail = (Block) pred.getBlock();
                if (binding_irdom.block_dominates(head.ptr, tail.ptr) != 0) {
                    loops.add(new Loop(head, tail));
                }
            }
        });

        for (var loop : loops) {
            // Collect all blocks that make up this loop.
            var blocksInLoop = new ArrayList<Block>();
            g.walkBlocks(block -> {
                if (binding_irdom.block_dominates(loop.head.ptr, block.ptr) != 0
                        && binding_irdom.block_dominates(block.ptr, loop.tail.ptr) != 0) {
                    blocksInLoop.add(block);
                }
            });
            var loopBlocks = new HashSet<>(blocksInLoop);

            var storesAndCallsInLoop = loopBlocks.stream()
                    .flatMap(block -> FirmUtils.blockContent(block).stream().filter(node -> node instanceof Store
                            || (node instanceof Call call && this.methodReferences.get(call) instanceof DefinedMethod)))
                    .collect(Collectors.toSet());
            var aliasInfo = new AliasAnalysis(nodeAstTypes);
            Function<Load, Boolean> isUnaliased = load -> storesAndCallsInLoop.stream().allMatch(store -> aliasInfo.guaranteedNotAliased(load, store));

            // Visit every node in the loop and check if it can be moved.
            // We do a DFS in the walker to guarantee that predecessors of a node are moved beforehand.
            var movableNodes = new HashMap<Node, Integer>();
            var movableLoads = new HashSet<Load>();
            var visited = new HashSet<Node>();
            for (var block : blocksInLoop) {
                for (var node : BackEdges.getOuts(block)) {
                    loopInvariantCodeMotionWalker(node.node, visited, isUnaliased, loopBlocks, loop.head, movableNodes, movableLoads);
                }
            }

            if (!movableNodes.isEmpty()) {
                // Find or create block to move nodes into.
                // This block needs to dominate and be postdominated by the loop head in order to avoid unnecessary calculation
                // of the moved nodes.

                var headPredsIndices = new ArrayList<Integer>();
                for (int i = 0; i < loop.head.getPredCount(); i++) {
                    var pred = loop.head.getPred(i);
                    if (pred instanceof Jmp || pred instanceof Proj) {
                        if (!pred.getBlock().equals(loop.tail)) {
                            headPredsIndices.add(i);
                        }
                    }
                }

                assert headPredsIndices.size() == 1;

                var loopHeadPredecessor = loop.head.getPred(headPredsIndices.get(0));

                Block targetBlock;
                // We see if the dominating predecessor of the loop is a branch.
                // If so we want to insert a new block between the this block and the loop head to avoid partial redundancies.
                var controlFlowSuccessor = StreamSupport.stream(BackEdges.getOuts(loopHeadPredecessor.getBlock()).spliterator(), false)
                        .filter(succ -> succ.node instanceof Block)
                        .count();
                if (controlFlowSuccessor > 1) {
                    targetBlock = (Block) this.g.newBlock(new Node[]{loopHeadPredecessor});
                    var jmp = this.g.newJmp(targetBlock);
                    loop.head.setPred(headPredsIndices.get(0), jmp);
                } else {
                    targetBlock = (Block) loopHeadPredecessor.getBlock();
                }

                while (!movableNodes.isEmpty()) {
                    var pair = movableNodes.entrySet().stream().filter(p -> p.getValue() >= 2).findAny();

                    if (pair.isEmpty()) {
                        break;
                    }

                    var nodeToBeMoved = pair.get().getKey();
                    movableNodes.remove(nodeToBeMoved);

                    this.moveNodeOutsideLoop(nodeToBeMoved, targetBlock, movableNodes);
                }


                var memPhiLoops = FirmUtils.blockContent(loop.head).stream().filter(node -> node instanceof Phi phi && phi.getMode().equals(Mode.getM())).toList();
                assert memPhiLoops.size() <= 1;
                var memPhiLoop = (Phi) memPhiLoops.get(0);
                var memPhiPredIdx = headPredsIndices.get(0);
                for (var load : movableLoads) {
                    this.moveLoadOutsideLoop(load, memPhiLoop, memPhiPredIdx, targetBlock, movableNodes);
                }
            }
        }

        BackEdges.disable(g);
    }

    public void traverseMemoryPath(Node node, Set<Node> visited) {
        if (visited.contains(node)) {
            return;
        }
        visited.add(node);

        if (node instanceof Proj proj
                && proj.getPred() instanceof Call call
                && !this.methodReferences.containsKey(call)
                && BackEdges.getNOuts(call) == 1
        ) {
            var mem = call.getMem();
            var bad = g.newBad(Mode.getM());
            binding_irgmod.exchange(node.ptr, bad.ptr);
            binding_irgmod.exchange(bad.ptr, mem.ptr);
            this.traverseMemoryPath(mem, visited);
        } else {
            switch (node) {
                case Proj proj -> this.traverseMemoryPath(proj.getPred(), visited);
                case Store store -> this.traverseMemoryPath(store.getMem(), visited);
                case Load load -> this.traverseMemoryPath(load.getMem(), visited);
                case Div div -> this.traverseMemoryPath(div.getMem(), visited);
                case Mod mod -> this.traverseMemoryPath(mod.getMem(), visited);
                case Call call -> this.traverseMemoryPath(call.getMem(), visited);
                case Phi phi -> phi.getPreds().forEach(pred -> this.traverseMemoryPath(pred, visited));
                case Start ignored -> {}
                default -> throw new AssertionError("Unexpected node on memory path.");
            }
        }
    }

    public void eliminateUnusedAllocs() {
        BackEdges.enable(this.g);

        var visited = new HashSet<Node>();

        this.g.getEndBlock().getPreds().forEach(pred -> {
            var ret = (Return) pred;
            this.traverseMemoryPath(ret.getMem(), visited);
        });

        BackEdges.disable(this.g);
    }
}
