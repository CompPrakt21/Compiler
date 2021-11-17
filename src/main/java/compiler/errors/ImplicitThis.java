package compiler.errors;

import compiler.ast.MethodCallExpression;
import compiler.ast.Reference;
import compiler.ast.VariableDefinition;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class ImplicitThis {
    public static class ImplicitThisMethodCall extends CompilerError {
        private final MethodCallExpression callExpr;

        public ImplicitThisMethodCall(MethodCallExpression expr) {
            this.callExpr = expr;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Implicit 'this' in static method");
            this.addPrimaryAnnotation(callExpr.getSpan());
        }
    }

    public static class ImplicitThisFieldCall extends CompilerError {
        private final Reference ref;
        private final VariableDefinition varDef;

        public ImplicitThisFieldCall(Reference ref, VariableDefinition varDef) {
            this.ref = ref;
            this.varDef = varDef;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Implicit 'this' in reference");
            this.addPrimaryAnnotation(ref.getSpan());
            this.addSecondaryAnnotation(varDef.getIdentifier().getSpan());
        }
    }
}
