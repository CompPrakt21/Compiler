package compiler.errors;

import compiler.ast.Expression;
import compiler.ast.Method;
import compiler.ast.MethodCallExpression;
import compiler.ast.Parameter;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class MethodParameterErrors {
    public static class DifferentLength extends CompilerError {
        private final Method method;
        private final MethodCallExpression methodCall;

        public DifferentLength(Method method, MethodCallExpression methodCall) {
            this.method = method;
            this.methodCall = methodCall;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Method definition and method call differ in length.");

            var argLength = this.methodCall.getArguments().size();
            this.addPrimaryAnnotation(methodCall.getSpanWithoutTarget(), String.format("has %s argument%s", argLength, argLength == 1 ? "" : "s"));
            var paramLength = this.method.getParameters().size();
            this.addSecondaryAnnotation(method.getParametersSpan(), String.format("has %s parameter%s", paramLength, paramLength == 1 ? "" : "s"));
        }
    }

    public static class ArgumentTypeMismatch extends CompilerError {
        private final Parameter param;
        private final Expression argument;

        private final Ty paramTy;
        private final TyResult argumentTy;

        public ArgumentTypeMismatch(Parameter param, Expression argument, Ty paramTy, TyResult argumentTy) {
            this.param = param;
            this.argument = argument;
            this.paramTy = paramTy;
            this.argumentTy = argumentTy;
        }

        @Override
        public void generate(Source source) {
            this.setMessage(String.format("Argument with type '%s' can not be given to parameter with type '%s'.", this.argumentTy, this.paramTy));

            this.addPrimaryAnnotation(this.argument.getSpan(), String.format("this has type '%s'", this.argumentTy));
            this.addSecondaryAnnotation(this.param.getSpan(), String.format("this expects type '%s'", this.paramTy));
        }
    }
}
