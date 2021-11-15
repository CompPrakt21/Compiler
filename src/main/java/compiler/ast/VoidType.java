package compiler.ast;

import compiler.Token;

import java.util.List;

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
