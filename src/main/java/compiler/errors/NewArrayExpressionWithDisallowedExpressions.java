package compiler.errors;

import compiler.Token;
import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessage;

public class NewArrayExpressionWithDisallowedExpressions extends CompilerError {
    public NewArrayExpressionWithDisallowedExpressions(Expression expression, Token expressionStartToken) {
        super("Expression is not allowed in larger array dimensions.");

        var span = expression != null ? expression.getSpan() : expressionStartToken.getSpan();
        this.addPrimaryAnnotation(span);
    }
}
