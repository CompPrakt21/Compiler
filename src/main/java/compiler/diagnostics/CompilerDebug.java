package compiler.diagnostics;

public non-sealed class CompilerDebug extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_DEBUG;

    public CompilerDebug(String message) {
        super(message, style);
    }
}
