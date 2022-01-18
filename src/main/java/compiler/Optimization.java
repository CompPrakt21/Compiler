package compiler;

import compiler.utils.FirmUtils;
import firm.BackEdges;
import firm.BlockWalker;
import firm.Graph;
import firm.Mode;
import firm.bindings.binding_irgraph;
import firm.bindings.binding_irnode;
import firm.nodes.*;

import java.nio.Buffer;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Optimization {

    private Graph g;
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
        o.simplifyArithmeticExpressions();
        o.constantFolding();
        o.eliminateRedundantSideEffects();
        o.eliminateRedundantPhis();
        o.eliminateSingletonBlocks();
    }

    public void constantFolding() {
        Map<Node, DataFlow.ConstantValue> values = DataFlow.analyzeConstantFolding(g);
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
                    case DataFlow.Unknown u -> g.newConst(0, pred.getMode());
                    case DataFlow.Constant c -> g.newConst(c.value);
                    case DataFlow.Variable vx -> pred;
                    default -> throw new AssertionError("Did not expect null here");
                };
                if (pred.getMode().equals(folded.getMode())) {
                    changes.add(new Change(n, i, folded));
                }
            }
        }
        for (Change c : changes) {
            c.node.setPred(c.predIdx, c.folded);
        }
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
                };
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
            for (int i = 0; i < n.getPredCount(); i++) {
                Node pred = n.getPred(i);
                if (!(pred instanceof Phi p)) {
                    continue;
                }
                List<Node> in = FirmUtils.preds(p);
                assert in.size() > 0;
                Node first = in.get(0);
                if (!in.stream().allMatch(input -> first.equals(input))) {
                    continue;
                }
                n.setPred(i, first);
            }
        }
    }

    public void eliminateSingletonBlocks() {
        updateBlockPhis();
        BackEdges.enable(g);
        g.walkBlocksPostorder(block -> {
            List<Node> content = StreamSupport.stream(BackEdges.getOuts(block).spliterator(), false)
                    .map(e -> e.node)
                    .filter(n -> n.getBlock().equals(block))
                    .collect(Collectors.toList());
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
        // x + (-y) = x - y
        // x - 0 = x
        // 0 - x = -x
        // x - x = 0
        // x - (-y) = x + y
        // (-x) - y = -(x + y)
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
            Mode mode = n.getMode();
            Node b = n.getBlock();
            List<BackEdges.Edge> parents = FirmUtils.backEdges(n);
            Node nn;
            boolean nChanged = false;
            do {
                nn = null;
                if (n instanceof Minus m1 && m1.getOp() instanceof Minus m2) {
                    // --x = x
                    nn = m2.getOp();
                } else if (n instanceof Not n1 && n1.getOp() instanceof Not n2) {
                    // !!x = x
                    nn = n2.getOp();
                } else if (n instanceof Add a) {
                    Node left = a.getLeft();
                    Node right = a.getRight();
                    // x + 0 = x
                    Supplier<Optional<Node>> r1 = () -> commOp(left, right, (l, r) ->
                            r instanceof Const c && c.getTarval().isNull() ?
                                    l : null);
                    // x + (-y) = x - y
                    Supplier<Optional<Node>> r2 = () -> commOp(left, right, (l, r) ->
                            r instanceof Minus m ?
                                    g.newSub(b, l, m.getOp()) : null);
                    nn = r1.get().or(r2).orElse(null);
                } else if (n instanceof Sub s) {
                    Node l = s.getLeft();
                    Node r = s.getRight();
                    if (r instanceof Const c && c.getTarval().isNull()) {
                        // x - 0 = x
                        nn = l;
                    } else if (l instanceof Const c && c.getTarval().isNull()) {
                        // 0 - x = -x
                        nn = g.newMinus(b, r);
                    } else if (l.equals(r)) {
                        // x - x = 0
                        nn = g.newConst(0, mode);
                    } else if (r instanceof Minus m) {
                        // x - (-y) = x + y
                        nn = g.newAdd(b, l, m.getOp());
                    } else if (l instanceof Minus m) {
                        // (-x) - y = -(x + y)
                        Node addNode = g.newAdd(b, m.getOp(), r);
                        nn = g.newMinus(b, addNode);
                        worklist.addLast(addNode);
                    }
                } else if (n instanceof Mul m) {
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
}
