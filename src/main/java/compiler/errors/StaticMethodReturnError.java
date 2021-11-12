package compiler.errors;

import compiler.Token;
import compiler.ast.Type;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class StaticMethodReturnError extends CompilerError {
    private final Token staticToken;
    private final Type foundType;

    public StaticMethodReturnError(Type foundType, Token staticToken) {
        this.staticToken = staticToken;
        this.foundType = foundType;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Static methods have to have a void return type.");

        this.addPrimaryAnnotation(foundType.getSpan(), "return type is: " + foundType.getName());
        this.addSecondaryAnnotation(staticToken.getSpan(), "method is static");
    }
}
