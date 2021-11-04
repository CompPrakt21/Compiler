package compiler.errors;

import compiler.Token;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class StaticFieldError extends CompilerError {
    private Token staticToken;

    public StaticFieldError(Token staticToken) {
        this.staticToken = staticToken;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Fields can not be static.");

        this.addPrimaryAnnotation(staticToken.getSpan());
    }
}
