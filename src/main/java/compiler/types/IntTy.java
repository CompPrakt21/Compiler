package compiler.types;

public final class IntTy extends Ty {
    @Override
    public String toString() {
        return "int";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntTy;
    }
}
