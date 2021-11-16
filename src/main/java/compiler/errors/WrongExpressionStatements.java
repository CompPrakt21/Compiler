package compiler.errors;

import compiler.ast.AssignmentExpression;
import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class WrongExpressionStatements extends CompilerError {

    private final Expression expr;

    public WrongExpressionStatements(Expression expression) {this.expr = expression;}
    @Override
    public void generate(Source source) {
        this.setMessage("ExpressionsStatments may only be method calls or assignments. %s is not allowed", expr.getClass().getSimpleName());
        this.addPrimaryAnnotation(expr.getSpan());
    }
}
