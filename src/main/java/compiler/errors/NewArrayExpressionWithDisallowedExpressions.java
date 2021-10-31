package compiler.errors;

import compiler.Token;
import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.Source;

public class NewArrayExpressionWithDisallowedExpressions extends CompilerError {
    private final Expression expression;
    private final Token expressionStartToken;

    public NewArrayExpressionWithDisallowedExpressions(Expression expression, Token expressionStartToken) {
        this.expression = expression;
        this.expressionStartToken = expressionStartToken;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Expression is not allowed in larger array dimensions.");

        var span = expression != null ? expression.getSpan() : expressionStartToken.getSpan();
        this.addPrimaryAnnotation(span);
    }
}
