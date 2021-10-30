package compiler.errors;

import compiler.diagnostics.CompilerWarning;

public class EmptyFileWarning extends CompilerWarning {
    public EmptyFileWarning() {
        super("The input file doesn't contain a program.");
    }
}
