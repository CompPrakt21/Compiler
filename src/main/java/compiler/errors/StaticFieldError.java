package compiler.errors;

import compiler.Token;
import compiler.diagnostics.CompilerError;

public class StaticFieldError extends CompilerError {
    public StaticFieldError(Token staticToken) {
        super("Fields can not be static.");

        this.addPrimaryAnnotation(staticToken.getSpan());
    }
}
