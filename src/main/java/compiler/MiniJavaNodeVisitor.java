package compiler;

import firm.nodes.*;

public interface MiniJavaNodeVisitor extends NodeVisitor {
    AssertionError err = new AssertionError("Encountered unexpected node");

    @Override
    default void visit(Align align) { throw err; }

    @Override
    default void visit(Alloc alloc) { throw err; }

    @Override
    default void visit(Anchor anchor) { throw err; }

    @Override
    default void visit(And and) { throw err; }

    @Override
    default void visit(Bad bad) { throw err; }

    @Override
    default void visit(Bitcast bitcast) { throw err; }

    @Override
    default void visit(Builtin builtin) { throw err; }

    @Override
    default void visit(Confirm confirm) { throw err; }

    @Override
    default void visit(CopyB copyB) { throw err; }

    @Override
    default void visit(Deleted deleted) { throw err; }

    @Override
    default void visit(Dummy dummy) { throw err; }

    @Override
    default void visit(Free free) { throw err; }

    @Override
    default void visit(IJmp iJmp) { throw err; }

    @Override
    default void visit(Id id) { throw err; }

    @Override
    default void visit(Mulh mulh) { throw err; }

    @Override
    default void visit(Mux mux) { throw err; }

    @Override
    default void visit(NoMem noMem) { throw err; }

    @Override
    default void visit(Offset offset) { throw err; }

    @Override
    default void visit(Or or) { throw err; }

    @Override
    default void visit(Pin pin) { throw err; }

    @Override
    default void visit(Raise raise) { throw err; }

    @Override
    default void visit(Sel sel) { throw err; }

    @Override
    default void visit(Shl shl) { throw err; }

    @Override
    default void visit(Shr shr) { throw err; }

    @Override
    default void visit(Shrs shrs) { throw err; }

    @Override
    default void visit(Switch aSwitch) { throw err; }

    @Override
    default void visit(Sync sync) { throw err; }

    @Override
    default void visit(Tuple tuple) { throw err; }

    @Override
    default void visitUnknown(Node node) { throw new AssertionError("visitUnknown was called"); }
}
