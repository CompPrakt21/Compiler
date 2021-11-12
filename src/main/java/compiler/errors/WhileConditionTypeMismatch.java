package compiler.errors;

import compiler.ast.WhileStatement;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.TyResult;

public class WhileConditionTypeMismatch extends CompilerError {
    private final WhileStatement stmt;
    private final TyResult conditionTy;

    public WhileConditionTypeMismatch(WhileStatement stmt, TyResult conditionTy) {
        this.stmt = stmt;
        this.conditionTy = conditionTy;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Condition of while statement is not a boolean");
        this.addPrimaryAnnotation(this.stmt.getCondition().getSpan(), String.format("this expression has type '%s'", conditionTy));
    }
}
