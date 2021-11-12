package compiler.ast;

import compiler.Token;

import java.util.List;

public final class IntType extends Type {
    public IntType(Token intType) {
        super();
        this.isError |= intType == null;
        setSpan(intType);
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return "int";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof IntType;
    }
}
