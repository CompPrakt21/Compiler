package compiler.errors;

import compiler.HasSpan;
import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessage;

import java.util.List;

public class NewObjectWithArgumentsError extends CompilerError {
    public NewObjectWithArgumentsError(Expression expression, List<Expression> arguments) {
        super("New object expression can not take any arguments.");

        this.addPrimaryAnnotation(new HasSpan.ListWrapper(arguments).getSpan());
    }
}
