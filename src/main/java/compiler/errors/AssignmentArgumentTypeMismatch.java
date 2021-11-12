package compiler.errors;

import compiler.ast.AssignmentExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.TyResult;

public class AssignmentArgumentTypeMismatch extends CompilerError {
    private AssignmentExpression expr;
    private TyResult lhsTy;
    private TyResult rhsTy;

    public AssignmentArgumentTypeMismatch(AssignmentExpression expr, TyResult lhsTy, TyResult rhsTy) {
        this.expr = expr;
        this.lhsTy = lhsTy;
        this.rhsTy = rhsTy;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Incompatible types in assignment expression.");
        this.addPrimaryAnnotation(this.expr.getLvalue().getSpan(), "the location stores values of type '%s'", this.lhsTy);
        this.addPrimaryAnnotation(this.expr.getRvalue().getSpan(), "this expression has type '%s'", this.rhsTy);
    }
}
