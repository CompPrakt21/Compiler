package compiler.ast;

import compiler.syntax.Token;

public final class VoidType extends Type {
    public VoidType(Token voidType) {
        super();
        setSpan(voidType);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof VoidType;
    }
}
