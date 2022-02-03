package compiler;

import firm.nodes.*;

public interface MiniJavaNodeVisitor extends NodeVisitor {
    private static void err(Node n) {
        throw new AssertionError("Encountered unexpected node: " + n);
    }

    @Override
    default void visit(Align align) { err(align); }

    @Override
    default void visit(Alloc alloc) { err(alloc); }

    @Override
    default void visit(Anchor anchor) { err(anchor); }

    @Override
    default void visit(Bad bad) { err(bad); }

    @Override
    default void visit(Bitcast bitcast) { err(bitcast); }

    @Override
    default void visit(Builtin builtin) { err(builtin); }

    @Override
    default void visit(Confirm confirm) { err(confirm); }

    @Override
    default void visit(CopyB copyB) { err(copyB); }

    @Override
    default void visit(Deleted deleted) { err(deleted); }

    @Override
    default void visit(Dummy dummy) { err(dummy); }

    @Override
    default void visit(Free free) { err(free); }

    @Override
    default void visit(IJmp iJmp) { err(iJmp); }

    @Override
    default void visit(Id id) { err(id); }

    @Override
    default void visit(Mulh mulh) { err(mulh); }

    @Override
    default void visit(Mux mux) { err(mux); }

    @Override
    default void visit(NoMem noMem) { err(noMem); }

    @Override
    default void visit(Offset offset) { err(offset); }

    @Override
    default void visit(Or or) { err(or); }

    @Override
    default void visit(Pin pin) { err(pin); }

    @Override
    default void visit(Raise raise) { err(raise); }

    @Override
    default void visit(Sel sel) { err(sel); }

    @Override
    default void visit(Sub sub) { err(sub); }

    @Override
    default void visit(Switch aSwitch) { err(aSwitch); }

    @Override
    default void visit(Sync sync) { err(sync); }

    @Override
    default void visit(Tuple tuple) { err(tuple); }

    @Override
    default void visitUnknown(Node node) { throw new AssertionError("visitUnknown was called"); }
}
