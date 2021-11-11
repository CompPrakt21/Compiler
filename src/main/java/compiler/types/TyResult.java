package compiler.types;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract sealed class TyResult permits Ty, VoidTy, UnresolveableTy {
    public TyResult map(Function<Ty, Ty> f) {
        if (this instanceof Ty ty) {
            return f.apply(ty);
        } else {
            return this;
        }
    }

    public void ifPresent(Consumer<Ty> c) {
        if (this instanceof Ty ty) {
            c.accept(ty);
        }
    }
}
