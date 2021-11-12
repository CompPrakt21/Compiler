package compiler.ast;

import compiler.Token;

import java.util.List;

public final class VoidType extends Type {
    public VoidType(Token voidType) {
        super();
        setSpan(voidType);
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
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof VoidType;
    }
}
