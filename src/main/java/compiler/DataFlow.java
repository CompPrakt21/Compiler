package compiler;

import firm.BackEdges;
import firm.Graph;
import firm.nodes.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataFlow {
    public interface MiniJavaNodeVisitor extends NodeVisitor {
        @Override
        default void visit(Align align) { }

        @Override
        default void visit(Alloc alloc) { }

        @Override
        default void visit(Anchor anchor) { }

        @Override
        default void visit(And and) { }

        @Override
        default void visit(Bad bad) { }

        @Override
        default void visit(Bitcast bitcast) { }

        @Override
        default void visit(Builtin builtin) { }

        @Override
        default void visit(Confirm confirm) { }

        @Override
        default void visit(CopyB copyB) { }

        @Override
        default void visit(Deleted deleted) { }

        @Override
        default void visit(Dummy dummy) { }

        @Override
        default void visit(Eor eor) { }

        @Override
        default void visit(Free free) { }

        @Override
        default void visit(IJmp iJmp) { }

        @Override
        default void visit(Id id) { }

        @Override
        default void visit(Mulh mulh) { }

        @Override
        default void visit(Mux mux) { }

        @Override
        default void visit(NoMem noMem) { }

        @Override
        default void visit(Offset offset) { }

        @Override
        default void visit(Or or) { }

        @Override
        default void visit(Pin pin) { }

        @Override
        default void visit(Raise raise) { }

        @Override
        default void visit(Sel sel) { }

        @Override
        default void visit(Shl shl) { }

        @Override
        default void visit(Shr shr) { }

        @Override
        default void visit(Shrs shrs) { }

        @Override
        default void visit(Switch aSwitch) { }

        @Override
        default void visit(Sync sync) { }

        @Override
        default void visit(Tuple tuple) { }

        @Override
        default void visit(firm.nodes.Unknown unknown) { }

        @Override
        default void visitUnknown(Node node) { }
    }

    public static class NodeCollector implements MiniJavaNodeVisitor {

        private ArrayDeque<Node> worklist;

        public NodeCollector(ArrayDeque<Node> worklist) {
            this.worklist = worklist;
        }

        @Override
        public void visit(Add add) {
            worklist.addLast(add);
        }

        @Override
        public void visit(Address address) {
            worklist.addLast(address);
        }

        @Override
        public void visit(Block block) {
            worklist.addLast(block);
        }

        @Override
        public void visit(Call call) {
            worklist.addLast(call);
        }

        @Override
        public void visit(Cmp cmp) {
            worklist.addLast(cmp);
        }

        @Override
        public void visit(Cond cond) {
            worklist.addLast(cond);
        }

        @Override
        public void visit(Const aConst) {
            worklist.addLast(aConst);
        }

        @Override
        public void visit(Conv conv) {
            worklist.addLast(conv);
        }

        @Override
        public void visit(Div div) {
            worklist.addLast(div);
        }

        @Override
        public void visit(End end) {
            worklist.addLast(end);
        }

        @Override
        public void visit(Jmp jmp) {
            worklist.addLast(jmp);
        }

        @Override
        public void visit(Load load) {
            worklist.addLast(load);
        }

        @Override
        public void visit(Member member) {
            worklist.addLast(member);
        }

        @Override
        public void visit(Minus minus) {
            worklist.addLast(minus);
        }

        @Override
        public void visit(Mod mod) {
            worklist.addLast(mod);
        }

        @Override
        public void visit(Mul mul) {
            worklist.addLast(mul);
        }

        @Override
        public void visit(Not not) {
            worklist.addLast(not);
        }

        @Override
        public void visit(Phi phi) {
            worklist.addLast(phi);
        }

        @Override
        public void visit(Proj proj) {
            worklist.addLast(proj);
        }

        @Override
        public void visit(Return aReturn) {
            worklist.addLast(aReturn);
        }

        @Override
        public void visit(Size size) {
            worklist.addLast(size);
        }

        @Override
        public void visit(Start start) {
            worklist.addLast(start);
        }

        @Override
        public void visit(Store store) {
            worklist.addLast(store);
        }

        @Override
        public void visit(Sub sub) {
            worklist.addLast(sub);
        }
    }

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
            values.put(aConst, new IntConstant(aConst.getTarval().asInt()));
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

        }

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
