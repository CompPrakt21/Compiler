package compiler.ast;

import java.util.List;

import compiler.utils.OptionalUtils;
import compiler.utils.StreamUtils;

public final class VoidType extends Type {
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
