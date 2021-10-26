package compiler.ast;

public final class EmptyStatement extends Statement {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof EmptyStatement;
    }
}
