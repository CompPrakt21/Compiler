package compiler.errors;

import compiler.ast.Expression;
import compiler.ast.MethodCallExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.types.Ty;
import compiler.types.TyResult;

public class MethodParameterErrors {
    public static class DifferentLength extends CompilerError {
        private final MethodDefinition method;
        private final MethodCallExpression methodCall;

        public DifferentLength(MethodDefinition method, MethodCallExpression methodCall) {
            this.method = method;
            this.methodCall = methodCall;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Method definition and method call differ in length.");

            var argLength = this.methodCall.getArguments().size();
            this.addPrimaryAnnotation(methodCall.getSpanWithoutTarget(), String.format("has %s argument%s", argLength, argLength == 1 ? "" : "s"));

            if (this.method instanceof DefinedMethod m) {
                var astMethod = m.getAstMethod();
                var paramLength = astMethod.getParameters().size();
                this.addSecondaryAnnotation(astMethod.getParametersSpan(), String.format("has %s parameter%s", paramLength, paramLength == 1 ? "" : "s"));
            }
        }
    }

    public static class ArgumentTypeMismatch extends CompilerError {
        private final MethodDefinition method;
        private final int paramIdx;
        private final Expression argument;

        private final Ty paramTy;
        private final TyResult argumentTy;

        public ArgumentTypeMismatch(MethodDefinition methodDef, int paramIdx, Expression argument, Ty paramTy, TyResult argumentTy) {
            this.method = methodDef;
            this.paramIdx = paramIdx;
            this.argument = argument;
            this.paramTy = paramTy;
            this.argumentTy = argumentTy;
        }

        @Override
        public void generate(Source source) {
            this.setMessage(String.format("Argument with type '%s' can not be given to parameter with type '%s'.", this.argumentTy, this.paramTy));

            this.addPrimaryAnnotation(this.argument.getSpan(), "this has type '%s'", this.argumentTy);

            if (this.method instanceof DefinedMethod m) {
                this.addSecondaryAnnotation(m.getAstMethod().getParameters().get(this.paramIdx).getSpan(), "this expects type '%s'", this.paramTy);
            }
        }
    }
}
