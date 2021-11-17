package compiler.errors;

import compiler.ast.NewObjectExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class NewObjectStringUse extends CompilerError {
    private final NewObjectExpression expr;

    public NewObjectStringUse(NewObjectExpression expr) {this.expr = expr;}

    @Override
    public void generate(Source source) {
        this.setMessage("You may not create a new 'String' object");
        this.addPrimaryAnnotation(expr.getType().getSpan());
    }
}
