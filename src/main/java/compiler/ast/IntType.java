package compiler.ast;

import compiler.syntax.Token;

public final class IntType extends Type {
    public IntType(Token intType) {
        super();
        this.isError |= intType == null;
        setSpan(intType);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof IntType;
    }
}
