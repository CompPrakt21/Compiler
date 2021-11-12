package compiler.errors;

import compiler.ast.IfStatement;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.TyResult;

public class IfConditionTypeMismatch extends CompilerError {
    private final IfStatement stmt;
    private final TyResult conditionTy;

    public IfConditionTypeMismatch(IfStatement stmt, TyResult conditionTy) {
        this.stmt = stmt;
        this.conditionTy = conditionTy;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Condition of if statement is not a boolean");
        this.addPrimaryAnnotation(this.stmt.getCondition().getSpan(), "this expression has type '%s'", conditionTy);
    }
}
