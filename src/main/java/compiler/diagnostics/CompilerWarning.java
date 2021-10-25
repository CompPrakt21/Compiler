package compiler.diagnostics;

public non-sealed class CompilerWarning extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_WARNING;

    public CompilerWarning(String message) {
        super(message, style);
    }
}
