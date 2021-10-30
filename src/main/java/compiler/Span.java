package compiler;

public record Span(int start, int length) implements HasSpan {
    public int end() {
        return this.start + this.length;
    }

    public Span merge(Span other) {
        int start = Math.min(this.start, other.start);
        int end = Math.max(this.end(), other.end());

        return Span.fromStartEnd(start, end);
    }

    public boolean intersect(Span other) {
        return !(this.end() <= other.start() || other.end() <= this.start());
    }

    public boolean contains(int i) {
        return this.start() <= i && i < this.end();
    }

    public static Span fromStartEnd(int start, int end) {
        return new Span(start, end - start);
    }

    @Override
    public String toString() {
        return "Span{" +
                "start=" + start +
                ", length=" + length +
                '}';
    }

    @Override
    public Span getSpan() {
        return this;
    }
}
