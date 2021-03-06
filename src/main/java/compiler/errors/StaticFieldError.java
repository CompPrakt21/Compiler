package compiler.errors;

import compiler.syntax.Token;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class StaticFieldError extends CompilerError {
    private final Token staticToken;

    public StaticFieldError(Token staticToken) {
        this.staticToken = staticToken;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Fields can not be static.");

        this.addPrimaryAnnotation(staticToken.getSpan());
    }
}
