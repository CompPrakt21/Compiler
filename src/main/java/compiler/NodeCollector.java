package compiler;

import firm.Graph;
import firm.nodes.*;

import java.util.ArrayDeque;

public class NodeCollector implements MiniJavaNodeVisitor {
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
    public void visit(Eor eor) { worklist.addLast(eor); }

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

    @Override
    public void visit(firm.nodes.Unknown unknown) { worklist.addLast(unknown); }

    public static ArrayDeque<Node> run(Graph g) {
        ArrayDeque<Node> nodes = new ArrayDeque<>();
        g.walkTopological(new NodeCollector(nodes));
        return nodes;
    }
}
