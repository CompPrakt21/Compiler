package compiler.errors;

import compiler.ast.UnaryExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class UnaryExpressionTypeMismatch extends CompilerError {
    private final UnaryExpression expr;
    private final Ty expectedTy;
    private final TyResult actualType;

    public UnaryExpressionTypeMismatch(UnaryExpression expr, Ty expectedTy, TyResult actualType) {
        this.expr = expr;
        this.expectedTy = expectedTy;
        this.actualType = actualType;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Invalid type, expected '%s' but got '%s'", this.expectedTy, this.actualType);
        this.addPrimaryAnnotation(this.expr.getExpression().getSpan(), "this expression has type '%s'", this.actualType);
    }
}
