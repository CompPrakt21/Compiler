package compiler.errors;

import compiler.ast.Method;
import compiler.ast.MethodCallExpression;
import compiler.ast.Reference;
import compiler.ast.ThisExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class MainMethodProblems {

    public static class MainMethodMissing extends CompilerError {

        @Override
        public void generate(Source source) {
            this.setMessage("No main method was found");
        }
    }

    public static class MainMethodCalled extends CompilerError {
        private final MethodCallExpression expr;

        public MainMethodCalled(MethodCallExpression expr) {
            this.expr = expr;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Calling the main method is disallowed");
            this.addPrimaryAnnotation(expr.getSpan());
        }
    }

    public static class MultipleMainMethods extends CompilerError {
        private final Method firstMainMethod;
        private final Method secondMainMethod;

        public MultipleMainMethods(Method firstMainMethod, Method secondMainMethod) {
            this.firstMainMethod = firstMainMethod;
            this.secondMainMethod = secondMainMethod;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Multiple main methods found.");

            this.addSecondaryAnnotation(this.firstMainMethod.getSpan(), "first main method here.");
            this.addPrimaryAnnotation(this.secondMainMethod.getSpan(), "second main method here.");
        }
    }

    public static class StaticNonMainMethod extends CompilerError {
        private final Method method;

        public StaticNonMainMethod(Method method) {
            this.method = method;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Only the main method may be static");
            this.addPrimaryAnnotation(method.getSpan());
        }
    }

    public static class UsingArgs extends CompilerError {
        private final Reference reference;

        public UsingArgs(Reference reference) {
            this.reference = reference;
        }


        @Override
        public void generate(Source source) {
            this.setMessage("'args' may not be used in the main method");
            this.addPrimaryAnnotation(reference.getSpan());
        }
    }

    public static class ReferenceUsingStatic extends CompilerError {
        private final ThisExpression expr;

        public ReferenceUsingStatic(ThisExpression expr) {
            this.expr = expr;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("'This' may not be used in a static method.");
            this.addPrimaryAnnotation(expr.getSpan());
        }
    }


}
