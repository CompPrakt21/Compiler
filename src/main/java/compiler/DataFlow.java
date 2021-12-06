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

public class DataFlow {

    public static sealed class ConstantValue permits Unknown, Constant, Variable { }

    public static final class Variable extends ConstantValue {
        public static final Variable value = new Variable();
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Variable;
        }
    }

    public static final class Unknown extends ConstantValue {
        public static final Unknown value = new Unknown();
        @Override
        public boolean equals(Object obj) {
            return obj instanceof Unknown;
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
    }

    public static class ConstantFolder implements MiniJavaNodeVisitor {

        private Map<Node, ConstantValue> values;

        public ConstantFolder(Map<Node, ConstantValue> values) {
            this.values = values;
        }

        private void eval(Function<List<TargetValue>, TargetValue> eval, Node parent, Node... children) {
            if (Arrays.stream(children).anyMatch(n -> values.get(n) instanceof Unknown)) {
                values.put(parent, Unknown.value);
                return;
            }
            List<ConstantValue> args = Arrays.stream(children).map(values::get).collect(Collectors.toList());
            if (args.stream().allMatch(a -> a instanceof Constant)) {
                TargetValue result = eval.apply(args.stream().map(a -> ((Constant) a).value).collect(Collectors.toList()));
                values.put(parent, new Constant(result));
                return;
            }
            values.put(parent, Variable.value);
        }

        private void unaryEval(Function<TargetValue, TargetValue> eval, Node parent, Node... children) {
            this.eval(args -> eval.apply(args.get(0)), parent, children);
        }

        private void biEval(BiFunction<TargetValue, TargetValue, TargetValue> eval, Node parent, Node... children) {
            this.eval(args -> eval.apply(args.get(0), args.get(1)), parent, children);
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
            biEval(TargetValue::div, div, div.getLeft(), div.getRight());
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
            biEval(TargetValue::mod, mod, mod.getLeft(), mod.getRight());
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
            eval(args -> null, phi, null);
        }

        @Override
        public void visit(Proj proj) {

        }

        @Override
        public void visit(Return aReturn) {

        }

        @Override
        public void visit(Size size) {

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

    public static void analyzeConstantFolding(Graph g) {
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
            n.accept(f);
            if (oldValue.equals(values.get(n))) {
                for (BackEdges.Edge e : BackEdges.getOuts(n)) {
                    worklist.addLast(e.node);
                }
            }
        }
        BackEdges.disable(g);
    }
}
