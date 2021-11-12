package compiler.errors;

import compiler.ast.BinaryOpExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

import java.util.Optional;

public abstract class BinaryExpressionTypeMismatch {

    public static class IncomparableTypes extends CompilerError {
        private final BinaryOpExpression expr;

        private final TyResult actualLhsOperandTy;
        private final TyResult actualRhsOperandTy;

        public IncomparableTypes(BinaryOpExpression expr, TyResult actualLhsOperandTy, TyResult actualRhsOperandTy) {
            this.expr = expr;
            this.actualLhsOperandTy = actualLhsOperandTy;
            this.actualRhsOperandTy = actualRhsOperandTy;
        }


        @Override
        public void generate(Source source) {
            this.setMessage(String.format("Can not compare types '%s' and '%s'.", this.actualLhsOperandTy, this.actualRhsOperandTy));

            this.addPrimaryAnnotation(this.expr.getLhs().getSpan(), String.format("this has type '%s'", this.actualLhsOperandTy));
            this.addPrimaryAnnotation(this.expr.getRhs().getSpan(), String.format("this has type '%s'", this.actualRhsOperandTy));
        }
    }

    public static class InvalidTypesForOperator extends CompilerError {
        private final BinaryOpExpression expr;
        private final Ty expectedOperandTy;

        private final Optional<Ty> actualLhsOperandTy;
        private final Optional<Ty> actualRhsOperandTy;

        public InvalidTypesForOperator(BinaryOpExpression expr, Ty expectedOperandTy, Optional<Ty> actualLhsOperandTy, Optional<Ty> actualRhsOperandTy) {
            this.expr = expr;
            this.expectedOperandTy = expectedOperandTy;
            this.actualLhsOperandTy = actualLhsOperandTy;
            this.actualRhsOperandTy = actualRhsOperandTy;
        }

        @Override
        public void generate(Source source) {
            this.setMessage(String.format("Invalid types. %s expects operands of type '%s'.", this.expr.getOperator(), this.expectedOperandTy));

            this.actualLhsOperandTy.ifPresent(ty -> this.addPrimaryAnnotation(this.expr.getLhs().getSpan(), String.format("this has type '%s'", ty)));
            this.actualRhsOperandTy.ifPresent(ty -> this.addPrimaryAnnotation(this.expr.getRhs().getSpan(), String.format("this has type '%s'", ty)));
        }
    }

    public static class VoidOperand extends CompilerError {
        private final BinaryOpExpression expr;

        private final Optional<TyResult> actualLhsOperandTy;
        private final Optional<TyResult> actualRhsOperandTy;

        public VoidOperand(BinaryOpExpression expr, Optional<TyResult> actualLhsOperandTy, Optional<TyResult> actualRhsOperandTy) {
            this.expr = expr;
            this.actualLhsOperandTy = actualLhsOperandTy;
            this.actualRhsOperandTy = actualRhsOperandTy;
        }

        @Override
        public void generate(Source source) {
            this.setMessage(String.format("Operand of %s is void.", this.expr.getOperator()));

            if (this.actualLhsOperandTy.isEmpty()) {
                this.addPrimaryAnnotation(this.expr.getLhs().getSpan(), "this is void");
            }
            if (this.actualRhsOperandTy.isEmpty()) {
                this.addPrimaryAnnotation(this.expr.getRhs().getSpan(), "this is void");
            }
        }
    }
}
