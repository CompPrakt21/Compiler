package compiler.diagnostics;

import picocli.CommandLine;

import java.io.PrintWriter;

class MessagePrinter {
    private final PrintWriter writer;

    private final CompilerMessageStyle messageStyle;

    private final CompilerMessageReporter.Colors colors;

    MessagePrinter(PrintWriter writer, CompilerMessageStyle style, CompilerMessageReporter.Colors colors) {
        this.writer = writer;
        this.messageStyle = style;
        this.colors = colors;
    }

    void printRepeat(String s, int repetitions, CompilerMessage.AnnotationType type) {
        this.printWithAnnotationTypeStyle(s.repeat(repetitions), type);
    }

    void printRepeat(char c, int repetitions, CompilerMessage.AnnotationType type) {
        this.printWithAnnotationTypeStyle((c + "").repeat(repetitions), type);
    }

    void println() {
        this.writer.print("\n");
    }

    void printWhitespace(int length) {
        this.writer.print(" ".repeat(length));
    }

    void print(String s, CompilerMessage.AnnotationType type) {
        this.printWithAnnotationTypeStyle(s, type);
    }

    void print(String s) {
        this.writer.print(s);
    }

    void print(StyledString s) {
        for (var element : s.content) {
            this.printWithStyle(element.text(), element.style());
        }
    }

    void print(char c, CompilerMessage.AnnotationType type) {
        this.printWithAnnotationTypeStyle("" + c, type);
    }

    void printWithSeparatorStyle(String s) {
        this.printWithStyle(s, this.messageStyle.separatorStyle);
    }

    private void printWithAnnotationTypeStyle(String s, CompilerMessage.AnnotationType type) {
        this.printWithStyle(s, this.messageStyle.getStyleString(type));
    }

    void printWithStyle(String s, TextStyle style) {
        boolean styleEnabled = switch (this.colors) {
            case ON -> true;
            case OFF -> false;
            case AUTO -> CommandLine.Help.Ansi.AUTO.enabled();
        };

        if (styleEnabled) {
            var endStyleString = TextStyle.NONE;
            this.writer.format("%s%s%s", style.getEscapeString(), s, endStyleString.getEscapeString());
        } else {
            this.writer.print(s);
        }
    }
}
