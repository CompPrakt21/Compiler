package compiler.ast;

public final class ArrayType extends Type {
    private Type childType;

    public ArrayType(Type childType) {
        this.childType = childType;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ArrayType other)) {
            return false;
        }
        return childType.syntacticEq(other.childType);
    }
}
