package compiler.utils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class OptionalUtils {

    public static <A, B, C> Optional<C> combine(Optional<? extends A> a,
                                                Optional<? extends B> b,
                                                BiFunction<? super A, ? super B, ? extends C> combiner) {
        Objects.requireNonNull(combiner);
        if (a.isEmpty() || b.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(combiner.apply(a.get(), b.get()));
    }
}
