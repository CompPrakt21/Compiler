package compiler.errors;

import compiler.ast.Method;
import compiler.ast.MethodCallExpression;
import compiler.ast.ThisExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

import java.lang.invoke.MethodHandle;

public class MainMethodProblems {

    public static class MainMethodMissing extends CompilerError{

        @Override
        public void generate(Source source) {
            this.setMessage("No main method was found");
        }
    }

    public static class MainMethodCalled extends CompilerError {
        private final MethodCallExpression expr;

        public MainMethodCalled(MethodCallExpression expr) {this.expr = expr;}

        @Override
        public void generate(Source source) {
            this.setMessage("Calling the main method is disallowed");
            this.addPrimaryAnnotation(expr.getSpan());
        }
    }

    public static class MultipleStaticMethods extends CompilerError {
        private final Method method;

        public MultipleStaticMethods(Method method) {this.method = method;}

        @Override
        public void generate(Source source) {
            this.setMessage("Only the main method may be static");
            this.addPrimaryAnnotation(method.getSpan());
        }
    }

    public static class ReferenceUsingStatic extends CompilerError {
        private final ThisExpression expr;

        public ReferenceUsingStatic(ThisExpression expr) {this.expr = expr;}

        @Override
        public void generate(Source source) {
            this.setMessage("'This' may not be used in a static method.");
            this.addPrimaryAnnotation(expr.getSpan());
        }
    }



}
