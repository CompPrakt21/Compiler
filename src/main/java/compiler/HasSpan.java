package compiler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public interface HasSpan {
    record ListWrapper(List<? extends HasSpan> list) implements HasSpan {
        @Override
        public Span getSpan() {
            if (list == null) {
                return new Span(-1, 0);
            }
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(HasSpan::getSpan)
                    .filter(Objects::nonNull)
                    .filter(span -> span.length() > 0)
                    .reduce(Span::merge)
                    .orElse(new Span(-1, 0));
        }
    }

    record OptionalWrapper(Optional<? extends HasSpan> optional) implements HasSpan {
        @Override
        public Span getSpan() {
            return optional.stream()
                    .filter(Objects::nonNull)
                    .map(HasSpan::getSpan)
                    .filter(Objects::nonNull)
                    .filter(span -> span.length() > 0)
                    .reduce(Span::merge)
                    .orElse(new Span(-1, 0));
        }
    }

    Span getSpan();
}