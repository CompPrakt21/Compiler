package compiler.diagnostics;

public non-sealed class CompilerError extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_ERROR;

    public CompilerError(String message) {
        super(message, style);
    }
}
