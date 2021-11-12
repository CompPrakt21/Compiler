package compiler.diagnostics;

import compiler.Span;

import java.util.*;
import java.util.stream.Collectors;

class DisplayList {

    private static final int TAB_SIZE = 4;

    abstract static sealed class Annotation
            permits UnderlineAnnotation, ConnectorAnnotation, LabelAnnotation, MultiLineAnnotation {

        CompilerMessage.AnnotationType annotationType;

        public Annotation(CompilerMessage.AnnotationType annotationType) {
            this.annotationType = annotationType;
        }

        abstract Span getRange();
    }

    static final class MultiLineAnnotation extends Annotation {

        public enum MultiLineType {
            Beginning,
            End,
        }

        int column;
        boolean isUnderline;
        MultiLineType multiLineType;
        int sideConnectorColumn;


        public MultiLineAnnotation(
                int column,
                boolean isUnderline,
                MultiLineType multiLineType,
                int sideConnectorColumn,
                CompilerMessage.AnnotationType annotationType
        ) {
            super(annotationType);
            this.column = column;
            this.isUnderline = isUnderline;
            this.multiLineType = multiLineType;
            this.sideConnectorColumn = sideConnectorColumn;
        }

        @Override
        Span getRange() {
            return new Span(0, this.column + 1);
        }
    }

    static final class UnderlineAnnotation extends Annotation {
        Span range;

        public UnderlineAnnotation(Span range, CompilerMessage.AnnotationType annotationType) {
            super(annotationType);
            this.range = range;
        }

        @Override
        Span getRange() {
            return this.range;
        }
    }

    static final class ConnectorAnnotation extends Annotation {
        int column;

        public ConnectorAnnotation(int column, CompilerMessage.AnnotationType annotationType) {
            super(annotationType);
            this.column = column;
        }

        @Override
        Span getRange() {
            return new Span(column, 1);
        }
    }

    static final class LabelAnnotation extends Annotation {
        int startColumn;
        String label;

        public LabelAnnotation(int startColumn, String label, CompilerMessage.AnnotationType type) {
            super(type);
            this.startColumn = startColumn;
            this.label = label;
        }

        @Override
        Span getRange() {
            return new Span(this.startColumn, this.label.length());
        }
    }

    static final class SideConnectors {
        public List<Optional<CompilerMessage.AnnotationType>> connectors;

        public SideConnectors(List<Optional<CompilerMessage.AnnotationType>> connectors) {
            this.connectors = connectors;
        }

        public static SideConnectors of(CompilerMessage.AnnotationType... a) {
            var list = Arrays.stream(a).map(Optional::of).collect(Collectors.toList());
            return new SideConnectors(list);
        }

        public int size() {
            return this.connectors.size();
        }
    }

    abstract static sealed class DisplayLine
            permits SourceLine, RawLine {
    }

    static final class RawLine extends DisplayLine {
        StyledString content;
        boolean startAtSeparator;

        public RawLine(StyledString content, boolean startAtSeparator) {
            this.content = content;
            this.startAtSeparator = startAtSeparator;
        }
    }

    abstract static sealed class SourceLine extends DisplayLine
            permits CodeLine, AnnotationLine, FoldLine {
        SideConnectors sideConnectors;

        public SourceLine(SideConnectors sideConnectors) {
            this.sideConnectors = sideConnectors;
        }
    }

    static final class CodeLine extends SourceLine {
        String content;
        int lineNr;

        public CodeLine(String content, int lineNr, SideConnectors sideConnectors) {
            super(sideConnectors);
            this.content = content;
            this.lineNr = lineNr;
        }
    }

    static final class AnnotationLine extends SourceLine {
        List<Annotation> annotations;

        public AnnotationLine(List<Annotation> annotations, SideConnectors sideConnectors) {
            super(sideConnectors);
            this.annotations = annotations;
        }
    }

    static final class FoldLine extends SourceLine {
        public FoldLine(SideConnectors sideConnectors) {
            super(sideConnectors);
        }
    }

    private final List<DisplayLine> body;
    private final CompilerMessageStyle style;

    DisplayList(List<DisplayLine> body, CompilerMessageStyle style) {
        this.body = body;
        this.style = style;
    }

    void format(MessagePrinter out) {
        int leadingWhitespace = this.body.stream().filter(dl -> dl instanceof CodeLine)
                .map(dl -> countLeadingWhitespace(((CodeLine) dl).content))
                .min(Integer::compare)
                .orElse(0);

        int leftPaddingForLineNumbers = this.body.stream().filter(dl -> dl instanceof CodeLine)
                .map(dl -> {
                    var sourceLine = (CodeLine) dl;
                    return lineNumberToString(sourceLine.lineNr).length();
                }).max(Integer::compareTo).orElse(0);

        int leftPaddingForMultiLineConnectors = this.body.stream()
                .filter(dl -> dl instanceof SourceLine)
                .map(dl -> ((SourceLine) dl).sideConnectors.connectors.size())
                .max(Integer::compare)
                .orElse(0) + 1;

        String lastCodeLine = null;

        for (DisplayLine displayLine : this.body) {
            switch (displayLine) {
                case RawLine l -> {
                    if (l.startAtSeparator) {
                        out.printWhitespace(leftPaddingForLineNumbers);
                    }
                    out.print(l.content);
                }
                case CodeLine l -> {
                    this.printLineNumberAndSeparator(out, Optional.of(l.lineNr), leftPaddingForLineNumbers);
                    this.printSideConnectors(out, l, leftPaddingForMultiLineConnectors);

                    var transformedLine = l.content.replaceAll("\t", " ".repeat(TAB_SIZE));

                    out.print(transformedLine.substring(leadingWhitespace));
                    lastCodeLine = l.content;
                }
                case AnnotationLine l -> {
                    this.printLineNumberAndSeparator(out, Optional.empty(), leftPaddingForLineNumbers);

                    if (l.annotations.isEmpty()) {
                        break;
                    }

                    l.annotations.sort(Comparator.comparingInt(a -> a.getRange().start())
                    );

                    this.printSideConnectors(out, l, leftPaddingForMultiLineConnectors);

                    removeOverlappedConnectorAnnotations(l.annotations);

                    int currentCursorPos = 0;

                    for (int annotationIdx = 0; annotationIdx < l.annotations.size(); annotationIdx++) {
                        var annotation = l.annotations.get(annotationIdx);

                        assert annotationIdx <= 0 || !(annotation instanceof MultiLineAnnotation);

                        int transformedStart = columnAfterTransformedWhitespace(lastCodeLine, annotation.getRange().start());

                        int neededWhitespace = Integer.max(transformedStart - leadingWhitespace - currentCursorPos, 0);

                        out.printWhitespace(neededWhitespace);
                        currentCursorPos += neededWhitespace;

                        switch (annotation) {
                            case UnderlineAnnotation sa -> {
                                var length = transformedSpan(lastCodeLine, sa.range).length();
                                out.printRepeat(this.style.getUnderLine(sa.annotationType), length, sa.annotationType);
                                currentCursorPos += length;
                            }
                            case ConnectorAnnotation ca -> {
                                out.print("|", ca.annotationType);
                                currentCursorPos += 1;
                            }
                            case LabelAnnotation la -> {
                                out.print(la.label, la.annotationType);
                                currentCursorPos += la.label.length();
                            }
                            case MultiLineAnnotation mla -> {
                                int transformedMlaCol = columnAfterTransformedWhitespace(lastCodeLine, mla.column);
                                int whitespaceRemovedColumn = transformedMlaCol - leadingWhitespace;
                                char lastChar = mla.isUnderline ? this.style.getUnderLine(mla.annotationType) : '|';

                                out.printRepeat("_", whitespaceRemovedColumn, mla.annotationType);
                                out.print(lastChar, mla.annotationType);
                                currentCursorPos += whitespaceRemovedColumn + 1;
                            }
                        }
                    }
                }
                case FoldLine l -> {
                    this.printFoldedLinesIndicator(out, leftPaddingForLineNumbers);
                    this.printSideConnectors(out, l, leftPaddingForMultiLineConnectors);
                }
            }

            out.println();
        }
    }

    private void printSideConnectors(MessagePrinter out, SourceLine dl, int leftPaddingForMultilineConnectors) {

        record MultilineAnnotationInfo(char beginningChar, int sideColumn, CompilerMessage.AnnotationType type) {
        }

        Optional<MultilineAnnotationInfo> startsWithMultiline = Optional.empty();

        if (dl instanceof AnnotationLine al && !al.annotations.isEmpty() && al.annotations.get(0) instanceof MultiLineAnnotation mla) {
            char beginningChar = switch (mla.multiLineType) {
                case Beginning -> ' ';
                case End -> '_';
            };
            int sideColumn = mla.sideConnectorColumn;
            var type = mla.annotationType;
            startsWithMultiline = Optional.of(new MultilineAnnotationInfo(beginningChar, sideColumn, type));
        }

        for (int i = 0; i < leftPaddingForMultilineConnectors; i++) {

            Optional<CompilerMessage.AnnotationType> connector;
            if (i < dl.sideConnectors.size()) {
                connector = dl.sideConnectors.connectors.get(i);
            } else {
                connector = Optional.empty();
            }

            if (connector.isPresent()) {
                out.print("|", connector.get());
            } else {
                if (startsWithMultiline.isPresent()) {
                    var multilineInfo = startsWithMultiline.get();

                    if (i == multilineInfo.sideColumn()) {
                        out.print(multilineInfo.beginningChar, multilineInfo.type);
                    } else if (i > multilineInfo.sideColumn()) {
                        out.print("_", multilineInfo.type);
                    } else {
                        out.printWhitespace(1);
                    }

                } else {
                    out.printWhitespace(1);
                }
            }
        }
    }

    private void printLineNumberAndSeparator(MessagePrinter out, Optional<Integer> lineNumber, int leftPaddingForLineNumbers) {
        var lineNumberString = lineNumber.map(DisplayList::lineNumberToString).orElse("");
        out.printWhitespace(leftPaddingForLineNumbers - lineNumberString.length());
        out.printWithSeparatorStyle(lineNumberString);

        out.printWithSeparatorStyle(" | ");
    }

    void printFoldedLinesIndicator(MessagePrinter out, int leftPaddingForLineNumbers) {
        out.printWithSeparatorStyle("...");

        // We don't print " | " on such lines. Since both "..." and " | " are
        // three columns long, we can just add the line number column length.
        out.printWhitespace(leftPaddingForLineNumbers);
    }

    private static void removeOverlappedConnectorAnnotations(List<Annotation> annotations) {
        List<Integer> markForRemoval = new ArrayList<>();

        for (int i = 0; i < annotations.size(); i++) {
            var annotation = annotations.get(i);

            if (annotation instanceof ConnectorAnnotation connectorAnnotation) {
                for (int j = 0; j < annotations.size(); j++) {
                    if (j == i) continue;

                    var possiblyOverlappingAnnotation = annotations.get(j);

                    if (possiblyOverlappingAnnotation.getRange().contains(connectorAnnotation.column)) {
                        markForRemoval.add(i);
                        break;
                    }
                }
            }
        }

        Collections.reverse(markForRemoval);

        for (int idx : markForRemoval) {
            annotations.remove(idx);
        }
    }

    private static String lineNumberToString(int lineNr) {
        return String.valueOf(lineNr + 1);
    }

    private static int countLeadingWhitespace(String s) {
        int result = 0;
        for (char c : s.toCharArray()) {
            if (c == '\t') result += TAB_SIZE;
            else if (Character.isWhitespace(c)) result += 1;
            else break;
        }
        return result;
    }

    private static int columnAfterTransformedWhitespace(String codeline, int columnBefore) {
        int columnAfter = 0;
        for (int i = 0; i < codeline.length(); i++) {
            if (i < columnBefore) {
                if (codeline.charAt(i) == '\t') {
                    columnAfter += TAB_SIZE;
                } else {
                    columnAfter += 1;
                }
            }
        }

        var diff = columnBefore - codeline.length();
        if (diff > 0) {
            columnAfter += diff;
        }

        return columnAfter;
    }

    private static Span transformedSpan(String codeline, Span before) {
        int transformedStart = columnAfterTransformedWhitespace(codeline, before.start());
        int transformedEnd = columnAfterTransformedWhitespace(codeline, before.end());
        return Span.fromStartEnd(transformedStart, transformedEnd);
    }
}
