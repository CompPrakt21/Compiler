package compiler.ast;

public final class ArrayType extends Type {
    private Type childType;

    public ArrayType(Type childType) {
        this.childType = childType;
    }
}
