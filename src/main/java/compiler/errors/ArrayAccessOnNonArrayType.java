package compiler.errors;

import compiler.ast.ArrayAccessExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

import java.util.Optional;

public class ArrayAccessOnNonArrayType extends CompilerError {
    private ArrayAccessExpression expr;
    private TyResult targetTy;

    public ArrayAccessOnNonArrayType(ArrayAccessExpression expr, TyResult targetTy) {
        this.expr = expr;
        this.targetTy = targetTy;
    }

    @Override
    public void generate(Source source) {

        if (this.targetTy instanceof Ty ty) {
            this.setMessage("Can not index type '%s'.", ty);
            this.addPrimaryAnnotation(this.expr.getTarget().getSpan(), "this has type '%s'", ty);
        } else {
            this.setMessage("Can not index this expression.");
            this.addPrimaryAnnotation(this.expr.getTarget().getSpan());
        }
    }
}
