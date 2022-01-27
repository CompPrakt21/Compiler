package compiler;

import compiler.types.Ty;
import firm.Entity;
import firm.Graph;
import firm.Mode;
import firm.Relation;
import firm.nodes.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class AliasAnalysis {

    private record AliasPair(Node a, Node b) {}

    private Map<AliasPair, Boolean> aliased = new HashMap<>();
    private Graph g;
    private Map<Node, Ty> nodeAstTypes;

    public AliasAnalysis(Graph g, Map<Node, Ty> nodeAstTypes) {
        this.g = g;
        this.nodeAstTypes = nodeAstTypes;
    }

    private boolean aliased(AliasPair ap, boolean aliased) {
        this.aliased.put(ap, aliased);
        return aliased;
    }

    private static boolean isArgsProj(Proj p) {
        if (!(p.getPred() instanceof Proj pArgs)) {
            return false;
        }
        if (!(pArgs.getMode().isValuesInMode(Mode.getT()))) {
            return false;
        }
        if (!(pArgs.getPred() instanceof Start)) {
            return false;
        }
        return true;
    }

    private static boolean isAllocProj(Proj p) {
        if (!(p.getPred() instanceof Proj pResult)) {
            return false;
        }
        if (!(pResult.getMode().isValuesInMode(Mode.getT()))) {
            return false;
        }
        if (!(pResult.getPred() instanceof Call c)) {
            return false;
        }
        if (!(c.getPtr() instanceof Address a)) {
            return false;
        }
        if (!a.getEntity().getName().contains("__builtin_alloc_function__")) {
            return false;
        }
        return true;
    }

    private boolean aliased(Node a, Node b) {
        AliasPair ap = new AliasPair(a, b);
        if (aliased.containsKey(ap)) {
            return aliased.get(ap);
        }
        switch (a) {
            case Member m1 -> {
                if (!(b instanceof Member m2)) {
                    // Members are not aliased with anything that isn't a member
                    // - except possibly the result of phi nodes.
                    return aliased(ap, b instanceof Phi);
                }
                Entity e1 = m1.getEntity();
                Entity e2 = m2.getEntity();
                // Different members are not aliased
                if (!e1.getName().equals(e2.getName())) {
                    return aliased(ap, false);
                }
                // Members of different types are not aliased
                if (!e1.getOwner().equals(e2.getOwner())) {
                    return aliased(ap, false);
                }
                // It depends on the pointer. Members of structures at different pointers will always not be aliased.
                return aliased(m1.getPtr(), m2.getPtr());
            }
            case Proj p1 -> {
                if (!(b instanceof Proj p2)) {
                    // This is tricky. Because of constant folding, x[0] ends up with a Proj node.
                    // We now need to compare it to y[c].
                    if (b instanceof Add a2) {
                        // y is a different array from x
                        if (!aliased(p1, a2.getLeft())) {
                            return aliased(ap, false);
                        }
                        // c is a const != 0.
                        if (a2.getRight() instanceof Const) {
                            return aliased(ap, false);
                        }
                        return aliased(ap, true);
                    }
                    // Outside of the above, projs are not aliased with anything that isn't a proj
                    // - except possibly the result of phi nodes.
                    return aliased(ap, b instanceof Phi);
                }
                // Local allocs are never aliased with parameters
                if (isArgsProj(p1) && isAllocProj(p2) || isAllocProj(p1) && isArgsProj(p2)) {
                    return aliased(ap, false);
                }
                if (isAllocProj(p1) && isAllocProj(p2)) {
                    Call c1 = (Call) ((Proj) p1.getPred()).getPred();
                    Call c2 = (Call) ((Proj) p2.getPred()).getPred();
                    if (!c1.equals(c2)) {
                        return aliased(ap, false);
                    }
                }
                // One of the projections points to either a load or a function call result.
                if (!nodeAstTypes.containsKey(p1) || !nodeAstTypes.containsKey(p2)) {
                    // We don't have any types available.
                    return aliased(ap, true);
                }
                if (nodeAstTypes.get(p1).equals(nodeAstTypes.get(p2))) {
                    // Same types => No knowledge about aliasing
                    return aliased(ap, true);
                }
                return aliased(ap, false);
            }
            case Add a1 -> {
                if (!(b instanceof Add a2)) {
                    // This is tricky. Because of constant folding, x[0] ends up with a Proj node.
                    // We now need to compare it to y[c].
                    if (b instanceof Proj p2) {
                        // y is a different array from x
                        if (!aliased(p2, a1.getLeft())) {
                            return aliased(ap, false);
                        }
                        // c is a const != 0.
                        if (a1.getRight() instanceof Const) {
                            return aliased(ap, false);
                        }
                        return aliased(ap, true);
                    }
                    // Outside of the above, adds are not aliased with anything that isn't an add
                    // - except possibly the result of phi nodes.
                    return aliased(ap, b instanceof Phi);
                }
                if (!aliased(a1.getLeft(), a2.getLeft())) {
                    // Different arrays can never be aliased.
                    return aliased(ap, false);
                }
                Node r1 = a1.getRight();
                Node r2 = a2.getRight();
                if (r1 instanceof Const c1 && r2 instanceof Const c2) {
                    // If both offsets are unequal, the pointers are not aliased.
                    return aliased(ap, c1.getTarval().compare(c2.getTarval()).contains(Relation.Equal));
                }
                // One of the offsets isn't a constant and was loaded either from an argument, memory or a function call
                // result. Normally, we wouldn't know particularly much now - but one important special case is
                // xs[i] and xs[i+c], which we handle specially. The following matches that structure.
                if (!(r1 instanceof Mul m1 && r2 instanceof Mul m2)) {
                    // One is a constant, the other isn't. We don't know whether they are aliased.
                    return aliased(ap, true);
                }
                if (!(m1.getLeft() instanceof Conv c1 && m2.getLeft() instanceof Conv c2)) {
                    // This should never occur because of how we construct the graphs, but let's be cautious.
                    return aliased(ap, true);
                }
                BiFunction<Add, Proj, Boolean> inner = (add1, p2) -> {
                    if (add1.getRight() instanceof Const && add1.getLeft() instanceof Proj p1) {
                        // xs[i + c] and xs[i]
                        return aliased(ap, !p1.equals(p2));
                    }
                    if (add1.getLeft() instanceof Const && add1.getRight() instanceof Proj p1) {
                        // xs[c + i] and xs[i]
                        return aliased(ap, !p1.equals(p2));
                    }
                    return aliased(ap, true);
                };
                if (c1.getOp() instanceof Add add1 && c2.getOp() instanceof Proj p2) {
                    return inner.apply(add1, p2);
                }
                if (c1.getOp() instanceof Proj p1 && c2.getOp() instanceof Add add2) {
                    return inner.apply(add2, p1);
                }
                return aliased(ap, true);
            }
            case Const c1 -> {
                return aliased(ap, !c1.getTarval().isNull());
            }
            default -> {
                return aliased(ap, true);
            }
        }
    }

    public boolean guaranteedNotAliased(Node a, Node b) {
        return !aliased(a, b);
    }
}
