package compiler.types;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract sealed class TyResult permits Ty, VoidTy, UnresolveableTy {
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    public TyResult map(Function<Ty, Ty> f) {
        if (this instanceof Ty ty) {
            return f.apply(ty);
        } else {
            return this;
        }
    }
}
