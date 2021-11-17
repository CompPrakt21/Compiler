package compiler.errors;

import compiler.ast.Expression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerWarning;
import compiler.diagnostics.Source;

public class ConstantError {
    public static class LiteralTooLarge extends CompilerError {
        private final Expression literal;

        public LiteralTooLarge(Expression lit) {
            this.literal = lit;
        }

        @Override
        public void generate(Source source) {
            String value = source.getSpanString(this.literal.getSpan());

            this.setMessage("Value '%s' does not fit into a signed 32bit integer.", value);

            this.addPrimaryAnnotation(this.literal.getSpan());
        }
    }

    public static class ExpressionTooLarge extends CompilerWarning {
        private final Expression expr;
        private final long value;

        public ExpressionTooLarge(Expression expr, long value) {
            this.expr = expr;
            this.value = value;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Constant expression evaluates to value too large for signed 32bit integer.");

            var digits = Math.round(Math.log(Math.abs(value)));

            if (digits > 10) {
                this.addPrimaryAnnotation(this.expr.getSpan(), "this has constant value %s.", this.value);
            } else if (this.value > 0) {
                this.addPrimaryAnnotation(this.expr.getSpan(), "this constant value is larger than 10^%s.", digits);
            } else {
                this.addPrimaryAnnotation(this.expr.getSpan(), "this constant value is smaller than -10^%s.", digits);
            }
        }
    }

    public static class DivisonByZero extends CompilerWarning {
        private final Expression zeroDivider;
        private final Expression division;

        public DivisonByZero(Expression zeroDivider, Expression division) {
            this.zeroDivider = zeroDivider;
            this.division = division;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Attempt to divide by zero.");

            this.addPrimaryAnnotation(this.division.getSpan());
            this.addSecondaryAnnotation(this.zeroDivider.getSpan(), "this is zero.");
        }
    }
}
