package compiler.errors;

import compiler.Span;
import compiler.ast.NewObjectExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class ClassDoesNotExist extends CompilerError {

    String ident;
    Span span;

    public ClassDoesNotExist(String ident, Span span) {
        this.ident = ident;
        this.span = span;
    }

    @Override
    public void generate(Source source) {
        this.setMessage(String.format("Class with name '%s' does not exist.", this.ident));
        this.addPrimaryAnnotation(this.span);
    }
}
