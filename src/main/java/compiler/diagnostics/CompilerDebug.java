package compiler.diagnostics;

public abstract non-sealed class CompilerDebug extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_DEBUG;

    public CompilerDebug(String message) {
        super(style);
    }
}
