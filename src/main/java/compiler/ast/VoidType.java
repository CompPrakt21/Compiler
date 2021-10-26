package compiler.ast;

import compiler.utils.OptionalUtils;
import compiler.utils.StreamUtils;

public final class VoidType extends Type {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof VoidType;
    }
}
