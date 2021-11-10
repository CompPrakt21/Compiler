package compiler.errors;

import compiler.ast.ArrayAccessExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;

import java.util.Optional;

public class ArrayAccessOnNonArrayType extends CompilerError {
    private ArrayAccessExpression expr;
    private Optional<Ty> targetTy;

    public ArrayAccessOnNonArrayType(ArrayAccessExpression expr, Ty targetTy) {
        this.expr = expr;
        this.targetTy = Optional.of(targetTy);
    }

    public ArrayAccessOnNonArrayType(ArrayAccessExpression expr) {
        this.expr = expr;
        this.targetTy = Optional.empty();
    }

    @Override
    public void generate(Source source) {
        if (this.targetTy.isPresent()) {
            this.setMessage(String.format("Can not index type '%s'.", this.targetTy.get()));
            this.addPrimaryAnnotation(this.expr.getTarget().getSpan(), String.format("this has type '%s'", this.targetTy.get()));
        } else {
            this.setMessage("Can not index this expression.");
            this.addPrimaryAnnotation(this.expr.getTarget().getSpan());
        }
    }
}
