package compiler.errors;

import compiler.ast.NewArrayExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;
import compiler.types.UnresolveableTy;
import compiler.types.VoidTy;

public class IllegalNewArrayExpression extends CompilerError {

    private TyResult ty;
    private NewArrayExpression expr;

    public IllegalNewArrayExpression(TyResult ty, NewArrayExpression expr) {
        this.ty = ty;
        this.expr = expr;
    }

    @Override
    public void generate(Source source) {
        this.setMessage(String.format("Can not create new array object with this type."));

        var msg = switch (this.ty) {
            case UnresolveableTy ignored -> "Type is unresolveable";
            case VoidTy ignored -> "Type is void";
            case Ty ty -> String.format("Type is '%s'", ty);
        };

        this.addPrimaryAnnotation(this.expr.getType().getSpan(), msg);
    }
}
