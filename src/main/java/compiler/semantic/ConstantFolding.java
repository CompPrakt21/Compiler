package compiler.semantic;

import compiler.ast.Class;
import compiler.ast.*;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.diagnostics.CompilerWarning;
import compiler.errors.ConstantError;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ConstantFolding {

    private final AstData<Integer> constants;

    private final Optional<CompilerMessageReporter> reporter;

    private boolean successful;

    public ConstantFolding(Optional<CompilerMessageReporter> reporter) {
        this.reporter = reporter;
        this.constants = new SparseAstData<>();
        this.successful = true;
    }

    private void reportError(CompilerError msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
        this.successful = false;
    }

    private void reportWarning(CompilerWarning msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    public record ConstantFoldingResult(boolean successful, AstData<Integer> constants) {
    }

    public static ConstantFoldingResult performConstantFolding(Program program, Optional<CompilerMessageReporter> reporter) {
        ConstantFolding folder = new ConstantFolding(reporter);

        for (Class klass : program.getClasses()) {
            for (Method method : klass.getMethods()) {
                folder.contantFoldStatement(method.getBody());
            }
        }

        return new ConstantFoldingResult(folder.successful, folder.constants);
    }

    public void contantFoldStatement(Statement statement) {
        switch (statement) {
            case Block block -> block.getStatements().forEach(this::contantFoldStatement);
            case EmptyStatement ignored -> {
            }
            case IfStatement ifstmt -> {
                constantFoldExpression(ifstmt.getCondition());
                contantFoldStatement(ifstmt.getThenBody());
                if (ifstmt.getElseBody().isPresent()) contantFoldStatement(ifstmt.getElseBody().get());
            }
            case ExpressionStatement expr -> constantFoldExpression(expr.getExpression());
            case WhileStatement whileStmt -> {
                constantFoldExpression(whileStmt.getCondition());
                contantFoldStatement(whileStmt.getBody());
            }
            case ReturnStatement returnStmt -> returnStmt.getExpression().ifPresent(this::constantFoldExpression);
            case LocalVariableDeclarationStatement decl -> decl.getInitializer().ifPresent(this::constantFoldExpression);
        }
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public void constantFoldExpression(Expression expression) {
        switch (expression) {
            case AssignmentExpression expr -> {
                constantFoldExpression(expr.getLvalue());
                constantFoldExpression(expr.getRvalue());
            }
            case BinaryOpExpression binop -> {
                constantFoldExpression(binop.getLhs());
                constantFoldExpression(binop.getRhs());

                var lhsConstant = this.constants.get(binop.getLhs());
                var rhsConstant = this.constants.get(binop.getRhs());

                if (lhsConstant.isPresent() && rhsConstant.isPresent()) {
                    long lhs = lhsConstant.get();
                    long rhs = rhsConstant.get();

                    long result = Long.MAX_VALUE;

                    switch (binop.getOperator()) {
                        case Addition -> result = lhs + rhs;
                        case Subtraction -> result = lhs - rhs;
                        case Division -> {
                            if (rhs == 0) {
                                reportWarning(new ConstantError.DivisonByZero(binop, binop.getRhs()));
                            } else {
                                result = lhs / rhs;
                            }
                        }
                        case Multiplication -> result = lhs * rhs;
                        case Modulo -> {
                            if (rhs == 0) {
                                reportWarning(new ConstantError.DivisonByZero(binop, binop.getRhs()));
                            }
                            result = lhs & rhs;
                        }
                        default -> {
                        }
                    }

                    if (result != Long.MAX_VALUE) {
                        try {
                            var exactResult = Math.toIntExact(result);
                            this.constants.set(binop, exactResult);
                        } catch (ArithmeticException e) {
                            reportWarning(new ConstantError.ExpressionTooLarge(binop, result));
                        }
                    }
                }
            }
            case UnaryExpression unary -> {
                constantFoldExpression(unary.getExpression());
                switch (unary.getOperator()) {
                    case LogicalNot -> {
                    }
                    case Negate -> {
                        var childConstant = this.constants.get(unary.getExpression());

                        if (childConstant.isPresent()) {
                            long result = childConstant.get();
                            long signedResult = -result;
                            try {
                                int exactResult = Math.toIntExact(signedResult);
                                this.constants.set(unary, exactResult);
                            } catch (ArithmeticException ignored) {
                                reportWarning(new ConstantError.ExpressionTooLarge(unary, signedResult));
                            }
                        }
                    }
                }
            }
            case MethodCallExpression methodCall -> {
                methodCall.getTarget().ifPresent(this::constantFoldExpression);
                methodCall.getArguments().forEach(this::constantFoldExpression);
            }
            case FieldAccessExpression ignored -> {
            }
            case ArrayAccessExpression arrayAccess -> {
                constantFoldExpression(arrayAccess.getTarget());
                constantFoldExpression(arrayAccess.getIndexExpression());
            }
            case BoolLiteral ignored -> {
            }
            case IntLiteral intLit -> {
                try {
                    long value = Long.parseLong(intLit.getValue());

                    if (intLit.getMinusToken().isPresent()) {
                        value *= -1;
                    }

                    int exactValue = Math.toIntExact(value);
                    this.constants.set(intLit, exactValue);
                } catch (NumberFormatException | ArithmeticException ignored) {
                    reportError(new ConstantError.LiteralTooLarge(intLit));
                }
            }
            case ThisExpression ignored -> {
            }
            case NewObjectExpression ignored -> {
            }
            case NewArrayExpression newArray -> constantFoldExpression(newArray.getFirstDimensionSize());
            case Reference ignored -> {
            }
            case NullExpression ignored -> {
            }
        }
    }
}
