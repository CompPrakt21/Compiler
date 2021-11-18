package compiler.diagnostics;

import compiler.syntax.Span;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Source {
    private final String sourceCode;

    private final List<Integer> lineOffsets;

    Source(String sourceCode) {
        this.sourceCode = sourceCode;

        ArrayList<Integer> lineOffsets = new ArrayList<>();

        lineOffsets.add(0); // First line starts at index 0

        for (int idx = 0; idx < sourceCode.length(); idx++) {

            var curChar = sourceCode.charAt(idx);

            if (curChar == '\r') {

                if (idx + 1 < sourceCode.length() && sourceCode.charAt(idx + 1) == '\n') {
                    idx += 1;
                }

                lineOffsets.add(idx + 1);

            } else if (curChar == '\n') {
                lineOffsets.add(idx + 1);
            }
        }

        this.lineOffsets = lineOffsets;
    }

    record SourceLocation(int line, int column) {
    }

    SourceLocation getSourceLocation(int byteOffset) {
        int line = Collections.binarySearch(this.lineOffsets, byteOffset);

        // Check docs for binarySearch...
        if (line < 0) {
            var insertionPoint = -(line + 1);
            line = insertionPoint - 1;
        }

        int lineBeginning = this.lineOffsets.get(line);

        // We assume every byte represents one character
        int col = (byteOffset - lineBeginning);
        int column = Math.toIntExact(col);

        return new SourceLocation(line, column);
    }

    Span getLineSpan(int line) {
        int start = this.lineOffsets.get(line);
        int end = line + 1 < this.lineOffsets.size() ? this.lineOffsets.get(line + 1) : this.sourceCode.length();

        return new Span(start, end - start);
    }

    String getLine(int line) {
        var lineSpan = this.getLineSpan(line);

        return this.sourceCode.substring(lineSpan.start(), lineSpan.end());
    }

    public String getSpanString(Span span) {
        return this.sourceCode.substring(span.start(), span.end());
    }
}
