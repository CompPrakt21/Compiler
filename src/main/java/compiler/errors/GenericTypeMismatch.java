package compiler.errors;

import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class GenericTypeMismatch extends CompilerError {
    private final Expression expr;
    private final Ty expectedTy;
    private final TyResult actualTy;

    public GenericTypeMismatch(Expression expr, Ty expectedTy, TyResult actualTy) {
        this.expr = expr;
        this.expectedTy = expectedTy;
        this.actualTy = actualTy;
    }

    @Override
    public void generate(Source source) {
        this.setMessage(String.format("Invalid types, expected type '%s', but got '%s'", this.expectedTy, this.actualTy));
        this.addPrimaryAnnotation(this.expr.getSpan(), String.format("this is type '%s'", this.actualTy));
    }
}
