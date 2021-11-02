package compiler.ast;

import compiler.Token;

import java.util.List;

public final class BoolType extends Type {
    public BoolType(Token token) {
        super();
        setSpan(token);
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return "Boolean";
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
        return otherAst instanceof BoolType;
    }
}
