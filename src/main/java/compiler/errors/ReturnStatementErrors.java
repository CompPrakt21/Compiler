package compiler.errors;

import compiler.ast.Method;
import compiler.ast.ReturnStatement;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class ReturnStatementErrors {
    public static class TypeMismatch extends CompilerError {
        private final Method method;
        private final TyResult expectedTy;
        private final ReturnStatement returnStmt;
        private final TyResult actualTy;

        public TypeMismatch(Method method, TyResult expectedTy, ReturnStatement returnStmt, TyResult actualTy) {
            this.method = method;
            this.expectedTy = expectedTy;
            this.returnStmt = returnStmt;
            this.actualTy = actualTy;
        }

        @Override
        public void generate(Source source) {
            assert this.returnStmt.getExpression().isPresent();

            this.setMessage("Invalid types, '%s' expects type '%s', but got '%s'", this.method.getIdentifier(), this.expectedTy, this.actualTy);
            this.addPrimaryAnnotation(this.returnStmt.getExpression().get().getSpan(), "this has type '%s'", actualTy);
            this.addSecondaryAnnotation(this.method.getReturnType().getSpan(), "expects type '%s'", expectedTy);
        }
    }

    public static class UnexpectedReturnExpr extends CompilerError {
        private final Method method;
        private final ReturnStatement returnStmt;

        public UnexpectedReturnExpr(Method method, ReturnStatement returnStmt) {
            this.method = method;
            this.returnStmt = returnStmt;
        }

        @Override
        public void generate(Source source) {
            assert this.returnStmt.getExpression().isPresent();

            this.setMessage("Unexpected expression in return statement.");
            this.addPrimaryAnnotation(this.returnStmt.getExpression().get().getSpan());
            this.addSecondaryAnnotation(this.method.getReturnType().getSpan(), "method doesn't expect a returned value.");
        }
    }

    public static class MissingReturnExpr extends CompilerError {
        private final Method method;
        private final ReturnStatement returnStmt;
        private final Ty expectedTy;

        public MissingReturnExpr(Method method, ReturnStatement returnStmt, Ty expectedTy) {
            this.method = method;
            this.returnStmt = returnStmt;
            this.expectedTy = expectedTy;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Return statements is missing an expression.");

            this.addPrimaryAnnotation(this.returnStmt.getSpan(), "Missing expression after return.");
            this.addSecondaryAnnotation(this.method.getReturnType().getSpan(), "expecting type '%s'", this.expectedTy);
        }
    }
}
