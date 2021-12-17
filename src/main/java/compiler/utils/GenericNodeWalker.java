package compiler.utils;

import firm.Graph;
import firm.nodes.*;

import java.util.function.Consumer;

public class GenericNodeWalker {
    public static void walkNodes(Graph graph, Consumer<Node> f) {
        graph.walk(new Visitor(f));
    }

    private record Visitor(Consumer<Node> fun) implements NodeVisitor {

        @Override
        public void visit(Add node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Address node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Align node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Alloc node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Anchor node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(And node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Bad node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Bitcast node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Block node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Builtin node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Call node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Cmp node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Cond node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Confirm node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Const node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Conv node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(CopyB node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Deleted node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Div node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Dummy node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(End node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Eor node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Free node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(IJmp node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Id node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Jmp node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Load node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Member node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Minus node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Mod node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Mul node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Mulh node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Mux node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(NoMem node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Not node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Offset node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Or node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Phi node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Pin node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Proj node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Raise node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Return node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Sel node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Shl node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Shr node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Shrs node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Size node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Start node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Store node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Sub node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Switch node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Sync node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Tuple node) {
            this.fun.accept(node);
        }

        @Override
        public void visit(Unknown node) {
            this.fun.accept(node);
        }

        @Override
        public void visitUnknown(Node node) {
            this.fun.accept(node);
        }
    }
}