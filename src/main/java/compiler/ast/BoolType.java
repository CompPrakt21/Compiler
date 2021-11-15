package compiler.ast;

import compiler.Token;

import java.util.List;

public final class BoolType extends Type {
    public BoolType(Token token) {
        super();
        setSpan(token);
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof BoolType;
    }
}
