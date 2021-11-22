package compiler.types;

public final class BoolTy extends Ty {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof BoolTy;
    }

    @Override
    public String toString() {
        return "bool";
    }

    @Override
    public int hashCode() {
        return 182277479; // random integer.
    }
}
