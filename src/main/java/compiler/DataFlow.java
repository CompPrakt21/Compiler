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

    public static sealed class ConstantValue permits Unknown, IntConstant, BoolConstant, Variable { }

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

    public static final class IntConstant extends ConstantValue {
        public int value;
        public IntConstant(int value) {
            this.value = value;
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof IntConstant v && value == v.value;
        }
    }

    public static final class BoolConstant extends ConstantValue {
        public boolean value;
        public BoolConstant(boolean value) {
            this.value = value;
        }
        @Override
        public boolean equals(Object obj) {
            return obj instanceof BoolConstant v && value == v.value;
        }
    }

    public static class ConstantFolder implements MiniJavaNodeVisitor {

        private Map<Node, ConstantValue> values;

        public ConstantFolder(Map<Node, ConstantValue> values) {
            this.values = values;
        }

        private void eval(Function<List<ConstantValue>, ConstantValue> eval, Node parent, Node... children) {
            if (Arrays.stream(children).anyMatch(n -> values.get(n) instanceof Unknown)) {
                values.put(parent, Unknown.value);
                return;
            }
            List<ConstantValue> args = Arrays.stream(children).map(values::get).collect(Collectors.toList());
            if (args.stream().allMatch(a -> a instanceof IntConstant || a instanceof BoolConstant)) {
                ConstantValue result = eval.apply(args);
                values.put(parent, result);
                return;
            }
            values.put(parent, Variable.value);
        }

        private void intEval(Function<List<Integer>, Integer> eval, Node parent, Node... children) {
            Function<List<ConstantValue>, ConstantValue> intEval = args -> {
                List<Integer> intArgs = args.stream().map(v -> ((IntConstant) v).value).collect(Collectors.toList());
                return new IntConstant(eval.apply(intArgs));
            };
            this.eval(intEval, parent, children);
        }

        private void unaryIntEval(Function<Integer, Integer> eval, Node parent, Node... children) {
            this.intEval(args -> eval.apply(args.get(0)), parent, children);
        }

        private void biIntEval(BiFunction<Integer, Integer, Integer> eval, Node parent, Node... children) {
            this.intEval(args -> eval.apply(args.get(0), args.get(1)), parent, children);
        }

        private void boolEval(Function<List<Boolean>, Boolean> eval, Node parent, Node... children) {
            Function<List<ConstantValue>, ConstantValue> boolEval = args -> {
                List<Boolean> boolArgs = args.stream().map(v -> ((BoolConstant) v).value).collect(Collectors.toList());
                return new BoolConstant(eval.apply(boolArgs));
            };
            this.eval(boolEval, parent, children);
        }

        private void unaryBoolEval(Function<Boolean, Boolean> eval, Node parent, Node... children) {
            this.boolEval(args -> eval.apply(args.get(0)), parent, children);
        }

        @Override
        public void visit(Add add) {
            biIntEval((a, b) -> a + b, add, add.getLeft(), add.getRight());
        }

        @Override
        public void visit(Address address) {

        }

        @Override
        public void visit(Block block) {

        }

        @Override
        public void visit(Call call) {

        }

        @Override
        public void visit(Cmp cmp) {

        }

        @Override
        public void visit(Cond cond) {

        }

        @Override
        public void visit(Const aConst) {
            TargetValue v = aConst.getTarval();
            Mode m = v.getMode();
            if (m.equals(Mode.getBu())) {
                values.put(aConst, new BoolConstant(v.asInt() == 1));
            } else {
                values.put(aConst, new IntConstant(v.asInt()));
            }

        }

        @Override
        public void visit(Conv conv) {

        }

        @Override
        public void visit(Div div) {
            biIntEval((a, b) -> a / b, div, div.getLeft(), div.getRight());
        }

        @Override
        public void visit(End end) {

        }

        @Override
        public void visit(Eor eor) {

        }

        @Override
        public void visit(Jmp jmp) {

        }

        @Override
        public void visit(Load load) {

        }

        @Override
        public void visit(Member member) {

        }

        @Override
        public void visit(Minus minus) {
            unaryIntEval(a -> -a, minus.getOp());
        }

        @Override
        public void visit(Mod mod) {
            biIntEval((a, b) -> a % b, mod.getLeft(), mod.getRight());
        }

        @Override
        public void visit(Mul mul) {
            biIntEval((a, b) -> a * b, mul.getLeft(), mul.getRight());
        }

        @Override
        public void visit(Not not) {
            unaryBoolEval(a -> !a, not.getOp()); }

        @Override
        public void visit(Phi phi) {

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

        }

        @Override
        public void visit(Store store) {

        }

        @Override
        public void visit(Sub sub) {
            biIntEval((a, b) -> a - b, sub.getLeft(), sub.getRight());
        }

        @Override
        public void visit(firm.nodes.Unknown unknown) {

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
