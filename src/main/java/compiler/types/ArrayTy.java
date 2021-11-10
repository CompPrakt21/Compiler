package compiler.types;

import java.util.Objects;

public final class ArrayTy extends Ty {
    private Ty childTy;

    public ArrayTy(Ty childTy, int dimensions) {
        if (dimensions == 0) {
            this.childTy = childTy;
        } else {
            this.childTy = new ArrayTy(childTy, dimensions - 1);
        }
    }

    public Ty getChildTy() {
        return childTy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayTy arrayTy = (ArrayTy) o;
        return Objects.equals(childTy, arrayTy.childTy);
    }
}
