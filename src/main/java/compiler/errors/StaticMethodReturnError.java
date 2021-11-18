package compiler.errors;

import compiler.syntax.Token;
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

        var typeStr = source.getSpanString(foundType.getSpan());

        this.addPrimaryAnnotation(foundType.getSpan(), "return type is '%s'", typeStr);
        this.addSecondaryAnnotation(staticToken.getSpan(), "method is static");
    }
}
