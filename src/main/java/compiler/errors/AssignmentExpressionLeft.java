package compiler.errors;

import compiler.ast.AssignmentExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.TyResult;

public class AssignmentExpressionLeft extends CompilerError {
    private final AssignmentExpression expr;

    public AssignmentExpressionLeft(AssignmentExpression expr) {
        this.expr = expr;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Only variables may be assigned a value");
        this.addPrimaryAnnotation(expr.getLvalue().getSpan());
    }
}
