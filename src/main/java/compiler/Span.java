package compiler;

public record Span(int start, int length) {
    public int end() {
        return this.start + this.length;
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
}
