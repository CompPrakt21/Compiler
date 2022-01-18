package compiler;

import firm.BackEdges;
import firm.Graph;
import firm.Mode;
import firm.TargetValue;
import firm.nodes.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DataFlow {

    public static sealed class ConstantValue permits Unknown, Constant, Variable {
        public ConstantValue sup(ConstantValue other) {
            return switch (this) {
                case Unknown u -> other;
                case Constant c ->
                        switch (other) {
                            case Unknown u -> c;
                            case Constant c2 -> c.value.compare(c2.value).value() == 1 ? c : Variable.value;
                            case Variable v -> v;
                            default -> throw new AssertionError("Impossible case");
                        };
                case Variable v -> v;
                default -> throw new AssertionError("Impossible case");
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

    public static class ConstantFolder implements MiniJavaNodeVisitor {

        private Map<Node, ConstantValue> values;

        public ConstantFolder(Map<Node, ConstantValue> values) {
            this.values = values;
        }

        private void evalAux(Function<List<TargetValue>, ConstantValue> eval, Node parent, Node... children) {
            if (Arrays.stream(children).anyMatch(n -> values.get(n) instanceof Unknown)) {
                values.put(parent, Unknown.value);
                return;
            }
            List<ConstantValue> args = Arrays.stream(children).map(values::get).collect(Collectors.toList());
            if (args.stream().allMatch(a -> a instanceof Constant)) {
                List<TargetValue> argValues = args.stream().map(a -> ((Constant) a).value).collect(Collectors.toList());
                values.put(parent, eval.apply(argValues));
                return;
            }
            values.put(parent, Variable.value);
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
            // TODO: Consider constant folding for boolean operations,
            // which involves reducing the graph
            block(cmp);
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
            block(conv);
        }

        @Override
        public void visit(Div div) {
            // Subtlety: If we divide by a constant 0, we optimize this side effect away.
            // TODO: Test what TargetValue.div does on 0
            biEval((a, b) -> b.asInt() == 0 ? new TargetValue(0, b.getMode()) : a.div(b), div, div.getLeft(), div.getRight());
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
            biEval((a, b) -> b.asInt() == 0 ? new TargetValue(0, b.getMode()) : a.mod(b), mod, mod.getLeft(), mod.getRight());
        }

        @Override
        public void visit(Mul mul) {
            biEval(TargetValue::mul, mul, mul.getLeft(), mul.getRight());
        }

        @Override
        public void visit(Not not) {
            // TODO: Constant folding for FIRM-internal booleans
            block(not);
        }

        @Override
        public void visit(Phi phi) {
            ConstantValue result = Unknown.value;
            for (Node pred : phi.getPreds()) {
                result = result.sup(values.get(pred));
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
        public void visit(Sub sub) {
            biEval(TargetValue::sub, sub, sub.getLeft(), sub.getRight());
        }

        @Override
        public void visit(firm.nodes.Unknown unknown) {
            // By default, nodes are marked with Unknown, so we don't need to insert it here.
        }

    }

    public static Map<Node, ConstantValue> analyzeConstantFolding(Graph g) {
        if (!BackEdges.enabled(g))
            BackEdges.enable(g);
        ArrayDeque<Node> worklist = new ArrayDeque<>();
        NodeCollector c = new NodeCollector(worklist);
        g.walkTopological(c);
        Map<Node, ConstantValue> values = worklist.stream()
                .collect(Collectors.toMap(node -> node, node -> new Unknown()));
        ConstantFolder f = new ConstantFolder(values);
        while (!worklist.isEmpty()) {
            Node n = worklist.removeFirst();
            ConstantValue oldValue = values.get(n);
            if (oldValue == null) {
                // This node wasn't picked up by our traversal at the start and is hence dead.
                continue;
            }
            n.accept(f);
            if (!oldValue.equals(values.get(n))) {
                for (BackEdges.Edge e : BackEdges.getOuts(n)) {
                    worklist.addLast(e.node);
                }
            }
        }
        BackEdges.disable(g);
        return values;
    }
}
