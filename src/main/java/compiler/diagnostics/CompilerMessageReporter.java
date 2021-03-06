package compiler.diagnostics;

import java.io.PrintWriter;

public class CompilerMessageReporter {
    private final Source source;
    private final PrintWriter output;

    private int errorsReported;

    private int warningsReported;

    private final Colors colors;

    public enum Colors {
        ON, AUTO, OFF
    }

    public CompilerMessageReporter(PrintWriter output, String source) {
        this(output, Colors.AUTO, source);
    }

    public CompilerMessageReporter(PrintWriter output, Colors colors, String source) {
        this.output = output;
        this.source = new Source(source);
        this.colors = colors;
    }

    public void reportMessage(CompilerMessage message) {
        switch (message) {
            case CompilerError ignored -> this.errorsReported += 1;
            case CompilerWarning ignored -> this.warningsReported += 1;
            case CompilerDebug ignored -> {
            }
            default -> throw new IllegalStateException("Unexpected value: " + message);
        }

        message.generate(this.source);

        try {
            var displayList = message.generateDisplayList(this.source);

            var output = new MessagePrinter(this.output, message.style, this.colors);

            displayList.format(output);

            output.println();

        } catch (Exception e) {
            // If error message generation fails, don't stop the program.
            e.printStackTrace(this.output);
        } finally {
            this.output.flush();
        }
    }

    /**
     * Signal that compilation has finished and no more errors will be reported.
     */
    public void finish() {
        if (this.errorsReported + this.warningsReported > 0) {
            var out = new MessagePrinter(this.output, CompilerError.style, this.colors);

            String errors = this.errorsReported != 0 ? String.format("%d error%s", this.errorsReported, this.errorsReported != 1 ? "s" : "") : "";
            String warnings = this.warningsReported != 0 ? String.format("%d warning%s", this.warningsReported, this.warningsReported != 1 ? "s" : "") : "";


            if (errors.length() > 0) {
                out.printWithStyle(errors, CompilerError.style.primaryStyle);
            }

            if (errors.length() > 0 && warnings.length() > 0) {
                out.printWithStyle(" and ", TextStyle.BOLD);
            }

            if (warnings.length() > 0) {
                out.printWithStyle(String.format("%d warning%s", this.warningsReported, this.warningsReported != 1 ? "s" : ""), CompilerWarning.style.primaryStyle);
            }

            if (errors.length() > 0 || warnings.length() > 0) {
                out.printWithStyle(" occurred while compiling.", TextStyle.BOLD);
                out.println();
            }

            this.output.flush();
        }
    }
}
