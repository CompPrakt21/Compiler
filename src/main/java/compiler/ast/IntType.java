package compiler.ast;

import java.util.List;

public final class IntType extends Type {
    @Override
    //TODO
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof IntType;
    }
}
