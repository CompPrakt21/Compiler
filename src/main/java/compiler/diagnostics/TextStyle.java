package compiler.diagnostics;

import picocli.CommandLine.Help.Ansi;

public class TextStyle {
    private final String escapeString;

    private TextStyle(Ansi.Style[] styles) {
        this.escapeString = Ansi.Style.on(styles);
    }

    String getEscapeString() {
        return this.escapeString;
    }

    static TextStyle BLUE_BOLD = new TextStyle(new Ansi.Style[]{Ansi.Style.fg_blue, Ansi.Style.bold});
    static TextStyle RED_BOLD = new TextStyle(new Ansi.Style[]{Ansi.Style.fg_red, Ansi.Style.bold});
    static TextStyle GREEN_BOLD = new TextStyle(new Ansi.Style[]{Ansi.Style.fg_green, Ansi.Style.bold});
    static TextStyle YELLOW_BOLD = new TextStyle(new Ansi.Style[]{Ansi.Style.fg_yellow, Ansi.Style.bold});
    static TextStyle BOLD = new TextStyle(new Ansi.Style[]{Ansi.Style.bold});
    static TextStyle NONE = new TextStyle(new Ansi.Style[]{Ansi.Style.reset});
}
