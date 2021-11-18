package compiler.ast;

import compiler.syntax.Token;

public final class ArrayType extends Type {
    private final Type childType;

    public ArrayType(Type childType, Token openBracket, Token closeBracked) {
        super();
        this.isError |= childType == null || openBracket == null || closeBracked == null;
        setSpan(childType, openBracket, closeBracked);

        this.childType = childType;
    }

    public Type getChildType() {
        return childType;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ArrayType other)) {
            return false;
        }
        return childType.syntacticEq(other.childType);
    }
}
