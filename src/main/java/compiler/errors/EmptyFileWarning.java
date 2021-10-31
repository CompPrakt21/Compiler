package compiler.errors;

import compiler.diagnostics.CompilerWarning;
import compiler.diagnostics.Source;

public class EmptyFileWarning extends CompilerWarning {
    @Override
    public void generate(Source source) {
        this.setMessage("The input file doesn't contain a program.");
    }
}
