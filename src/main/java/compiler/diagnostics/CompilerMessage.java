package compiler.diagnostics;

import compiler.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract sealed class CompilerMessage
        permits CompilerError, CompilerWarning, CompilerDebug {

    enum AnnotationType {
        PRIMARY,
        SECONDARY,
    }

    record Annotation(Span location, Optional<String> message, AnnotationType annotationType) {
    }

    private String message;

    private final List<String> notes;

    private final List<Annotation> annotations;

    final CompilerMessageStyle style;

    protected CompilerMessage(CompilerMessageStyle style) {
        this.message = "";
        this.notes = new ArrayList<>();
        this.annotations = new ArrayList<>();
        this.style = style;
    }

    public abstract void generate(Source source);

    public void setMessage(String fmt, Object... o) {
        this.message = String.format(fmt, o);
    }

    public void addNote(String note) {
        this.notes.add(note);
    }

    public void addPrimaryAnnotation(Span location) {
        this.annotations.add(new Annotation(location, Optional.empty(), AnnotationType.PRIMARY));
    }

    public void addPrimaryAnnotation(Span location, String fmt, Object... o) {
        this.annotations.add(new Annotation(location, Optional.of(String.format(fmt, o)), AnnotationType.PRIMARY));
    }

    public void addSecondaryAnnotation(Span location) {
        this.annotations.add(new Annotation(location, Optional.empty(), AnnotationType.SECONDARY));
    }

    public void addSecondaryAnnotation(Span location, String fmt, Object... o) {
        this.annotations.add(new Annotation(location, Optional.of(String.format(fmt, o)), AnnotationType.SECONDARY));
    }

    /*
     * The remaining code deals with transforming an instance of CompilerMessage to a DisplayList.
     */

    private abstract static sealed class ActiveAnnotation
            permits ActiveUnderlineAnnotation, ActiveLabelAnnotation, ActiveMultilineAnnotation {

        boolean startedConnector;

        Annotation origin;

        abstract Span getMinSpan();

        abstract int getConnectorColumn();


        public ActiveAnnotation(boolean startedConnector, Annotation origin) {
            this.startedConnector = startedConnector;
            this.origin = origin;
        }
    }

    private final static class ActiveUnderlineAnnotation extends ActiveAnnotation {
        Span range;
        Optional<String> message;
        boolean messageIsPlacedOnSameLine;

        public ActiveUnderlineAnnotation(Span range, Optional<String> message, Annotation origin) {
            super(false, origin);
            this.range = range;
            this.message = message;
            this.messageIsPlacedOnSameLine = false;
        }

        @Override
        Span getMinSpan() {
            if (this.messageIsPlacedOnSameLine) {
                assert this.message.isPresent();
                return new Span(this.range.start(), this.range.length() + 1 + this.message.get().length());
            } else {
                return this.range;
            }
        }

        @Override
        int getConnectorColumn() {
            return this.range.start();
        }
    }

    private final static class ActiveLabelAnnotation extends ActiveAnnotation {
        int column;
        String message;

        public ActiveLabelAnnotation(int column, String message, Annotation origin) {
            super(false, origin);
            this.column = column;
            this.message = message;
        }

        @Override
        Span getMinSpan() {
            return new Span(this.column, this.message.length());
        }

        @Override
        int getConnectorColumn() {
            return this.column;
        }
    }

    private final static class ActiveMultilineAnnotation extends ActiveAnnotation implements Cloneable {
        int column;
        Optional<String> message;
        DisplayList.MultiLineAnnotation.MultiLineType multiLineType;

        public ActiveMultilineAnnotation(
                int column,
                Optional<String> message,
                DisplayList.MultiLineAnnotation.MultiLineType multiLineType,
                Annotation origin
        ) {
            super(false, origin);
            this.column = column;
            this.message = message;
            this.multiLineType = multiLineType;
        }

        public ActiveMultilineAnnotation clone() {
            try {
                return (ActiveMultilineAnnotation) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException();
            }
        }

        @Override
        Span getMinSpan() {
            if (this.startedConnector) {
                return this.getLargeSpan();
            } else {
                return new Span(this.column, 1);
            }
        }

        @Override
        int getConnectorColumn() {
            return this.column;
        }

        Span getLargeSpan() {
            return new Span(
                    0,
                    this.column + 1 + this.message.map((String message) -> message.length() + 1).orElse(1)
            );
        }
    }

    private static final class SideConnectors {

        /**
         * Stores the corresponding ending multiline annotations for each active column;
         */
        List<Optional<Annotation>> activeSideConnectors;
        List<Optional<Annotation>> nextActiveSideConnectors;

        public SideConnectors() {
            this.activeSideConnectors = new ArrayList<>();
            this.nextActiveSideConnectors = new ArrayList<>();
        }

        public DisplayList.SideConnectors toDisplaySideConnectors() {
            var res = this.activeSideConnectors.stream().map((Optional<Annotation> oa) -> oa.map((Annotation a) -> a.annotationType)).collect(Collectors.toList());
            return new DisplayList.SideConnectors(res);
        }

        /**
         * @return side connector column where multiline annotation is placed.
         */
        public int addChange(ActiveMultilineAnnotation annotation) {
            return switch (annotation.multiLineType) {
                case Beginning -> this.startNewConnector(annotation.origin);
                case End -> this.stopConnector(annotation.origin);
            };
        }

        public void applyChanges() {
            this.activeSideConnectors.clear();
            this.activeSideConnectors.addAll(this.nextActiveSideConnectors);
        }

        private int startNewConnector(Annotation origin) {
            for (int i = 0; i < this.nextActiveSideConnectors.size(); i++) {
                var slot = this.nextActiveSideConnectors.get(i);
                if (slot.isEmpty()) {
                    this.nextActiveSideConnectors.set(i, Optional.of(origin));
                    return i;
                }
            }

            this.nextActiveSideConnectors.add(Optional.of(origin));
            return this.nextActiveSideConnectors.size() - 1;
        }

        private int stopConnector(Annotation origin) {
            for (int i = 0; i < this.nextActiveSideConnectors.size(); i++) {
                var slot = this.nextActiveSideConnectors.get(i);
                if (slot.isPresent() && slot.get() == origin) {
                    this.nextActiveSideConnectors.set(i, Optional.empty());
                    return i;
                }
            }

            throw new RuntimeException("Connection with provided origin was never started.");
        }
    }

    private void generateHeaderLines(List<DisplayList.DisplayLine> result) {
        StyledString header = new StyledString();
        header.add(this.style.headerStart, this.style.primaryStyle);
        header.add(this.message, TextStyle.BOLD);
        result.add(new DisplayList.RawLine(header, false));
    }

    private void generateSourceLines(Source source, List<DisplayList.DisplayLine> result) {
        if (this.annotations.isEmpty()) {
            return;
        }

        int firstStart = this.annotations.stream()
                .map((Annotation a) -> a.location.start())
                .min(Integer::compare)
                .orElseThrow(() -> new AssertionError("There have to be annotations"));
        int lastEnd = this.annotations.stream()
                .map((Annotation a) -> a.location.end())
                .max(Integer::compare)
                .orElseThrow(() -> new AssertionError("There have to be annotations"));

        int firstLine = source.getSourceLocation(firstStart).line();
        int lastLine = source.getSourceLocation(lastEnd).line();

        SideConnectors activeSideConnectors = new SideConnectors();

        result.add(new DisplayList.AnnotationLine(List.of(), activeSideConnectors.toDisplaySideConnectors()));

        for (int lineIdx = firstLine; lineIdx <= lastLine; lineIdx++) {

            var lineSpan = source.getLineSpan(lineIdx);
            String line = source.getLine(lineIdx).stripTrailing();

            // Sort Annotations into categories.
            /*
              |
              | some code line
              |____-
             */
            var beginningMultilineAnnotations = this.annotations.stream()
                    .filter((Annotation a) -> lineSpan.contains(a.location.start()) && !lineSpan.contains(a.location.end()))
                    .map((Annotation a) -> {
                        int col = source.getSourceLocation(a.location.start()).column();
                        return new ActiveMultilineAnnotation(col, Optional.empty(), DisplayList.MultiLineAnnotation.MultiLineType.Beginning, a);
                    })
                    .collect(Collectors.toList());
            /*
                some code line
               ______-
              |
             */
            var endingMultilineAnnotations = this.annotations.stream()
                    .filter((Annotation a) -> !lineSpan.contains(a.location.start()) && lineSpan.contains(a.location.end()))
                    .map((Annotation a) -> {
                        int col = source.getSourceLocation(a.location.end() - 1).column(); // we subtract minus one, because the end of a span is exclusive and we want the last inclusive column.
                        return new ActiveMultilineAnnotation(col, a.message, DisplayList.MultiLineAnnotation.MultiLineType.End, a);
                    })
                    .collect(Collectors.toList());
            /*
                some code line
                     ^^^^
             */
            var singleLineAnnotations = this.annotations.stream()
                    .filter((Annotation a) -> lineSpan.contains(a.location.start()) && lineSpan.contains(a.location.end()))
                    .map((Annotation a) -> {
                        int col = source.getSourceLocation(a.location.start()).column();
                        return new ActiveUnderlineAnnotation(new Span(col, a.location.length()), a.message, a);
                    })
                    .collect(Collectors.toList());

            // Emit current code line.
            result.add(new DisplayList.CodeLine(line, lineIdx, activeSideConnectors.toDisplaySideConnectors()));

            var activeAnnotations = new ArrayList<ActiveAnnotation>();
            activeAnnotations.addAll(singleLineAnnotations);
            activeAnnotations.addAll(endingMultilineAnnotations);
            activeAnnotations.addAll(beginningMultilineAnnotations);

            // Place underline annotations
            while (!activeAnnotations.isEmpty()) {

                // Order active annotations.
                activeAnnotations.sort((ActiveAnnotation a1, ActiveAnnotation a2) -> {
                    // First underline annotations (large to small)
                    // Then ending multiline annotations (first to last)
                    // then starting multiline annotations (first to last)
                    // Then label annotations

                    Function<ActiveAnnotation, Integer> determineAnnotationPriority = (ActiveAnnotation aa) -> switch (aa) {
                        case ActiveLabelAnnotation ignored -> 1;
                        case ActiveUnderlineAnnotation ignored1 -> 2;
                        case ActiveMultilineAnnotation a -> switch (a.multiLineType) {
                            case Beginning -> 3;
                            case End -> 4;
                        };
                    };

                    var a1Priority = determineAnnotationPriority.apply(a1);
                    var a2Priority = determineAnnotationPriority.apply(a2);

                    if (!(a1Priority.equals(a2Priority))) {
                        return a1Priority.compareTo(a2Priority);
                    } else {
                        switch (a1) {
                            case ActiveUnderlineAnnotation ignored -> {
                                var a1Length = a1.getMinSpan().length();
                                var a2Length = a2.getMinSpan().length();
                                return Integer.compare(a2Length, a1Length);
                            }
                            case ActiveMultilineAnnotation ignored -> {
                                var a1Start = a1.getMinSpan().start();
                                var a2Start = ((ActiveMultilineAnnotation) a2).getMinSpan().start();
                                return Integer.compare(a1Start, a2Start);
                            }
                            case ActiveLabelAnnotation ignored -> {
                                var a1Start = a1.getMinSpan().start();
                                var a2Start = ((ActiveLabelAnnotation) a2).getMinSpan().start();
                                return Integer.compare(a1Start, a2Start);
                            }
                        }
                    }

                    throw new AssertionError("Java doesn't realize the above if-switch construct is exhaustive.");
                });

                // Place (greedily) as many Annotations as possible in the next AnnotationLine.
                var annotationsInNextAnnotationLine = new ArrayList<ActiveAnnotation>();
                for (ActiveAnnotation unplacedActiveAnnotation : activeAnnotations) {
                    boolean canNotBePlaced = annotationsInNextAnnotationLine.stream()
                            .anyMatch((ActiveAnnotation placedActiveAnnotation) -> placedActiveAnnotation.getMinSpan().intersect(unplacedActiveAnnotation.getMinSpan()));

                    if (!canNotBePlaced) {
                        annotationsInNextAnnotationLine.add(unplacedActiveAnnotation);
                    }
                }

                // Remove placed Annotations from active Annotations.
                for (ActiveAnnotation placedAnnotation : annotationsInNextAnnotationLine) {
                    activeAnnotations.remove(placedAnnotation);
                }

                var displayAnnotations = new ArrayList<DisplayList.Annotation>(); // Emitted DisplayList.Annotations

                // For each placed Annotation generate the emitted DisplayList.Annotation and add new (different)
                // annotations to activeAnnotations, if necessary
                for (ActiveAnnotation placedAnnotation : annotationsInNextAnnotationLine) {
                    switch (placedAnnotation) {
                        case ActiveUnderlineAnnotation aua -> {

                            boolean messageIsPresentAndCanBePlaced = false;

                            if (aua.message.isPresent()) {
                                var message = aua.message.get();

                                var messageSpan = new Span(aua.range.end() + 1, message.length());

                                boolean messageObstructed = annotationsInNextAnnotationLine.stream()
                                        .anyMatch((ActiveAnnotation placedActiveAnnotation) -> placedActiveAnnotation.getMinSpan().intersect(messageSpan));

                                if (!messageObstructed) {
                                    aua.messageIsPlacedOnSameLine = true;
                                    messageIsPresentAndCanBePlaced = true;
                                }
                            }

                            displayAnnotations.add(new DisplayList.UnderlineAnnotation(aua.range, aua.origin.annotationType));

                            if (messageIsPresentAndCanBePlaced) {
                                displayAnnotations.add(new DisplayList.LabelAnnotation(aua.range.end() + 1, aua.message.get(), aua.origin.annotationType));
                            } else if (aua.message.isPresent()) {
                                var newActiveAnnotation = new ActiveLabelAnnotation(aua.range.start(), aua.message.get(), aua.origin);
                                newActiveAnnotation.startedConnector = true;
                                activeAnnotations.add(newActiveAnnotation);
                            }
                        }
                        case ActiveLabelAnnotation ala -> displayAnnotations.add(new DisplayList.LabelAnnotation(ala.column, ala.message, ala.origin.annotationType));
                        case ActiveMultilineAnnotation ama -> {
                            if (ama.startedConnector) {
                                var sideConnectorColumn = activeSideConnectors.addChange(ama);

                                var message = ama.message.filter((String s) -> ama.multiLineType == DisplayList.MultiLineAnnotation.MultiLineType.End);

                                displayAnnotations.add(new DisplayList.MultiLineAnnotation(ama.column, false, ama.multiLineType, sideConnectorColumn, ama.origin.annotationType));
                                if (message.isPresent()) {
                                    displayAnnotations.add(new DisplayList.LabelAnnotation(ama.column + 2, message.get(), ama.origin.annotationType));
                                }
                            } else {

                                boolean canNotBeFullyPlaced = annotationsInNextAnnotationLine.stream()
                                        .filter((ActiveAnnotation a) -> a != ama)
                                        .anyMatch((ActiveAnnotation placedActiveAnnotation) -> placedActiveAnnotation.getMinSpan().intersect(ama.getLargeSpan()));

                                if (canNotBeFullyPlaced) {
                                    displayAnnotations.add(new DisplayList.UnderlineAnnotation(new Span(ama.column, 1), ama.origin.annotationType));

                                    var copiedAma = ama.clone();
                                    copiedAma.startedConnector = true;
                                    activeAnnotations.add(copiedAma);
                                } else {
                                    var sideConnectorColumn = activeSideConnectors.addChange(ama);

                                    var message = ama.message.filter((String s) -> ama.multiLineType == DisplayList.MultiLineAnnotation.MultiLineType.End);

                                    displayAnnotations.add(new DisplayList.MultiLineAnnotation(ama.column, true, ama.multiLineType, sideConnectorColumn, ama.origin.annotationType));
                                    if (message.isPresent()) {
                                        displayAnnotations.add(new DisplayList.LabelAnnotation(ama.column + 2, message.get(), ama.origin.annotationType));
                                    }
                                }
                            }
                        }
                    }
                }

                // Connectors for every active annotation that might require them.
                // Obstructed connectors are filtered by DisplayList.
                for (ActiveAnnotation stillActiveAnnotation : activeAnnotations) {
                    if (stillActiveAnnotation.startedConnector) {
                        displayAnnotations.add(new DisplayList.ConnectorAnnotation(stillActiveAnnotation.getConnectorColumn(), stillActiveAnnotation.origin.annotationType));
                    }
                }

                result.add(new DisplayList.AnnotationLine(displayAnnotations, activeSideConnectors.toDisplaySideConnectors()));
                activeSideConnectors.applyChanges();
            }
        }

        reduceLongCodeLineChucks(result);
    }

    private void reduceLongCodeLineChucks(List<DisplayList.DisplayLine> result) {
        int codeLineChunkEnd = -1; // inclusive end.
        int codeLineChunkLength = 0;

        // Traversing list in reverse, so indices don't change once we remove elements.
        for (int i = result.size() - 1; i >= 0; i--) {
            var line = result.get(i);

            if (line instanceof DisplayList.CodeLine) {
                if (codeLineChunkEnd < 0) {
                    codeLineChunkEnd = i;
                    codeLineChunkLength = 1;
                } else {
                    codeLineChunkLength += 1;
                }
            } else {

                if (codeLineChunkEnd >= 0 && codeLineChunkLength > this.style.foldingSize) {
                    var halfFoldingSize = this.style.foldingSize / 2;

                    // Remove code lines and replace them with a single FoldLine.
                    int codeLineChunkStart = codeLineChunkEnd - codeLineChunkLength + 1;
                    var removedLines = result.subList(codeLineChunkStart + halfFoldingSize, codeLineChunkEnd + 1 - halfFoldingSize);
                    assert removedLines.size() > 0;
                    var sideConnectors = ((DisplayList.CodeLine) removedLines.get(0)).sideConnectors;
                    removedLines.clear();
                    result.add(codeLineChunkStart + halfFoldingSize, new DisplayList.FoldLine(sideConnectors));

                }

                codeLineChunkEnd = -1;
            }
        }
    }

    private void generateNoteLines(List<DisplayList.DisplayLine> result) {
        for (String note : this.notes) {

            var lines = note.lines().collect(Collectors.toList());

            for (int noteLineIdx = 0; noteLineIdx < lines.size(); noteLineIdx++) {
                var line = lines.get(noteLineIdx);

                StyledString s = new StyledString();

                if (noteLineIdx == 0) {
                    s.add(" = ", this.style.separatorStyle);
                    s.add("note: ", TextStyle.BOLD);
                } else {
                    s.add(" ".repeat(9), TextStyle.NONE);
                }

                s.add(line, TextStyle.NONE);
                result.add(new DisplayList.RawLine(s, true));
            }
        }
    }

    DisplayList generateDisplayList(Source source) {
        var result = new ArrayList<DisplayList.DisplayLine>();

        this.generateHeaderLines(result);
        this.generateSourceLines(source, result);
        this.generateNoteLines(result);

        return new DisplayList(result, this.style);
    }
}