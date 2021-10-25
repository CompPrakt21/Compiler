package compiler.diagnostics;

public class CompilerMessageStyle {

    String headerStart;

    char primaryUnderline;
    char secondaryUnderline;

    TextStyle primaryStyle;
    TextStyle secondaryStyle;

    TextStyle separatorStyle;

    /**
     * Start folding code lines once this many subsequent lines follow each other.
     */
    int foldingSize;

    CompilerMessageStyle(String headerStart, char primaryUnderline, char secondaryUnderline, TextStyle primaryStyle, TextStyle secondaryStyle, TextStyle separatorStyle, int foldingSize) {
        this.headerStart = headerStart;
        this.primaryUnderline = primaryUnderline;
        this.secondaryUnderline = secondaryUnderline;
        this.primaryStyle = primaryStyle;
        this.secondaryStyle = secondaryStyle;
        this.separatorStyle = separatorStyle;
        this.foldingSize = foldingSize;
    }

    TextStyle getStyleString(CompilerMessage.AnnotationType type) {
        return switch (type) {
            case PRIMARY -> this.primaryStyle;
            case SECONDARY -> this.secondaryStyle;
        };
    }

    char getUnderLine(CompilerMessage.AnnotationType type) {
        return switch (type) {
            case PRIMARY -> this.primaryUnderline;
            case SECONDARY -> this.secondaryUnderline;
        };
    }

    static final CompilerMessageStyle DEFAULT_ERROR = new CompilerMessageStyle(
            "ERROR: ",
            '^',
            '-',
            TextStyle.RED_BOLD,
            TextStyle.BLUE_BOLD,
            TextStyle.BLUE_BOLD,
            4
    );

    static final CompilerMessageStyle DEFAULT_WARNING = new CompilerMessageStyle(
            "WARNING: ",
            '^',
            '-',
            TextStyle.YELLOW_BOLD,
            TextStyle.BLUE_BOLD,
            TextStyle.BLUE_BOLD,
            4
    );

    static final CompilerMessageStyle DEFAULT_DEBUG = new CompilerMessageStyle(
            "DEBUG: ",
            '^',
            '-',
            TextStyle.GREEN_BOLD,
            TextStyle.BLUE_BOLD,
            TextStyle.BLUE_BOLD,
            4
    );
}
