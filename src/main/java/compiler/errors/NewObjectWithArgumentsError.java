package compiler.errors;

import compiler.HasSpan;
import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.Source;

import java.util.List;

public class NewObjectWithArgumentsError extends CompilerError {
    private final Expression expression;
    private final List<Expression> arguments;

    public NewObjectWithArgumentsError(Expression expression, List<Expression> arguments) {
        this.expression = expression;
        this.arguments = arguments;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("New object expression can not take any arguments.");

        this.addPrimaryAnnotation(new HasSpan.ListWrapper(arguments).getSpan());
    }
}
