package compiler.diagnostics;

public abstract non-sealed class CompilerWarning extends CompilerMessage {

    static final CompilerMessageStyle style = CompilerMessageStyle.DEFAULT_WARNING;

    public CompilerWarning() {
        super(style);
    }
}
