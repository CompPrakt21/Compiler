package compiler.errors;

import compiler.ast.UnaryExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class UnaryExpressionTypeMismatch extends CompilerError {
    private UnaryExpression expr;
    private Ty expectedTy;
    private TyResult actualType;

    public UnaryExpressionTypeMismatch(UnaryExpression expr, Ty expectedTy, TyResult actualType) {
        this.expr = expr;
        this.expectedTy = expectedTy;
        this.actualType = actualType;
    }

    @Override
    public void generate(Source source) {
        this.setMessage(String.format("Invalid type, expected '%s' but got '%s'", this.expectedTy, this.actualType));
        this.addPrimaryAnnotation(this.expr.getExpression().getSpan(), String.format("this expression has type '%s'", this.actualType));
    }
}
