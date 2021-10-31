package compiler.diagnostics;

public abstract non-sealed class CompilerError extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_ERROR;

    public CompilerError() {
        super(style);
    }
}
