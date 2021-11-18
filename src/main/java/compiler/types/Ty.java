package compiler.types;

public abstract sealed class Ty extends TyResult permits IntTy, BoolTy, ClassTy, NullTy, ArrayTy {

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public boolean comparable(Ty other) {
        var thisIsReferenceTy = this instanceof ClassTy || this instanceof NullTy || this instanceof ArrayTy;
        var otherIsReferenceTy = other instanceof ClassTy || other instanceof NullTy || other instanceof ArrayTy;

        if (thisIsReferenceTy && otherIsReferenceTy) {
            if (this instanceof NullTy || other instanceof NullTy) {
                return true;
            } else {
                return this.equals(other);
            }
        } else if (!thisIsReferenceTy && !otherIsReferenceTy) {
            return this.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.getClass().hashCode();
    }
}
