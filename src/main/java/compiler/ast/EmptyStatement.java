package compiler.ast;

import compiler.Token;

import java.util.List;

public final class EmptyStatement extends Statement {
    public EmptyStatement(Token semicolon) {
        super();
        setSpan(semicolon);
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean startsNewBlock() {
        return false;
    }

    @Override
    public String getVariable() {
        return null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof EmptyStatement;
    }
}
