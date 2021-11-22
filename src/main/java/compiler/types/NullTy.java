package compiler.types;

public final class NullTy extends Ty {
    @Override
    public boolean equals(Object obj) {
        return obj instanceof NullTy;
    }

    @Override
    public String toString() {
        return "null";
    }

    @Override
    public int hashCode() {
        return 536996823; // random integer
    }
}
