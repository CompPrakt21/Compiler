package compiler;

import compiler.utils.FirmUtils;
import firm.*;
import firm.bindings.binding_irdom;
import firm.bindings.binding_irgmod;
import firm.bindings.binding_irgraph;
import firm.bindings.binding_irnode;
import firm.nodes.*;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Optimization {

    private final Graph g;
    private Map<Block, List<Phi>> blockPhis;

    public Optimization(Graph g) {
        this.g = g;
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

    public static void optimizeFull(Graph g) {
        Optimization o = new Optimization(g);
        o.constantFolding();
        o.eliminateRedundantSideEffects();
        o.simplifyArithmeticExpressions();
        o.commonSubexpressionElimination();
        o.eliminateSingletonBlocks();
        o.eliminateTrivialConds();
        o.inlineTrivialBlocks();
        o.eliminateRedundantPhis();
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


    public record ExpressionNode(Node n) {
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
            return switch (n) {
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
        }
    }

    public void commonSubexpressionElimination() {
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
}
