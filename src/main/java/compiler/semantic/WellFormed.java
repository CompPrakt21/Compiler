package compiler.semantic;

import compiler.ast.*;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.NameResolution;
import compiler.types.ClassTy;
import compiler.types.IntrinsicClassTy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class WellFormed {

    private Optional<Method> mainMethod;
    private boolean inMainMethod;
    private int countedLocalVariablesInCurrentMethod;

    private final NameResolution.NameResolutionResult nameResolution;

    private AstData<Integer> countedLocalVariables; // Maps method definitions to the number of local variables they contain.
    boolean correct;

    private final Optional<CompilerMessageReporter> reporter;

    private WellFormed(Optional<CompilerMessageReporter> reporter, NameResolution.NameResolutionResult nameResolution) {
        this.nameResolution = nameResolution;

        this.inMainMethod = false;
        this.correct = true;
        this.countedLocalVariables = new SparseAstData<>();

        this.reporter = reporter;
        mainMethod = Optional.empty();
    }

    private void reportWarning(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
        correct = false;
    }

    public record WellFormedResult(boolean correct, AstData<Integer> variableCounts) {
    }

    public static WellFormedResult checkWellFormdness(Program program, NameResolution.NameResolutionResult nameResolution, Optional<CompilerMessageReporter> reporter) {
        var analysis = new WellFormed(reporter, nameResolution);

        analysis.checkProgram(program);

        return new WellFormedResult(analysis.correct, analysis.countedLocalVariables);
    }

    private boolean checkProgram(Program program) {
        for (var klass : program.getClasses()) {
            for (var method : klass.getMethods()) {
                checkMethod(method);
            }
        }

        if (mainMethod.isEmpty()) {
            reportError(new MainMethodProblems.MainMethodMissing());
        }

        for (var klass : program.getClasses()) {
            for (var method : klass.getMethods()) {
                inMainMethod = mainMethod.map(main -> method == main).orElse(false);
                countedLocalVariablesInCurrentMethod = 0;

                checkStatement(method.getBody());

                this.countedLocalVariables.set(method, countedLocalVariablesInCurrentMethod);
                inMainMethod = false;
            }
        }

        return correct;
    }

    private void checkMethod(Method method) {
        if (isMainMethod(method)) {
            if (mainMethod.isEmpty()) {
                mainMethod = Optional.of(method);
                inMainMethod = true;
            } else {
                reportError(new MainMethodProblems.MultipleMainMethods(mainMethod.get(), method));
            }
        } else if (method.isStatic()) {
            reportError(new MainMethodProblems.StaticNonMainMethod(method));
        }

        if (!(method.getReturnType() instanceof VoidType)) {
            if (checkReturnPathsInStatements(method.getBody().getStatements()).isEmpty()) {
                reportError(new ReturnStatementErrors.MissingReturnOnPath(method));
            }
        }
    }

    @SuppressWarnings("ConstantConditions")
    private List<ReturnStatement> checkReturnPathsInStatements(List<Statement> statements) {
        List<ReturnStatement> hasReturn = List.of();
        List<Statement> deadCode = new ArrayList<>();

        for (Statement statement : statements) {
            if (!hasReturn.isEmpty()) {
                deadCode.add(statement);

            } else {
                switch (statement) {
                    case Block block -> {
                        List<ReturnStatement> blockCheck = checkReturnPathsInStatements(block.getStatements());
                        hasReturn = blockCheck.isEmpty() ? List.of() : blockCheck;
                    }
                    case ReturnStatement rtnStmt -> hasReturn = List.of(rtnStmt);
                    case IfStatement ifstmt -> {
                        List<ReturnStatement> ifElse = hasReturnInIfElse(ifstmt);
                        hasReturn = ifElse.isEmpty() ? List.of() : ifElse;
                    }
                    default -> {
                    }
                }
            }

        }
        if (!deadCode.isEmpty()) reportWarning(new DeadCodeWarning(deadCode, hasReturn));
        return hasReturn;
    }

    private List<ReturnStatement> hasReturnInIfElse(IfStatement ifStatement) {
        if (ifStatement.getElseBody().isEmpty()) return List.of();

        List<ReturnStatement> thenList = checkReturnPathsInStatements(List.of(ifStatement.getThenBody()));
        List<ReturnStatement> elseList = checkReturnPathsInStatements(List.of(ifStatement.getElseBody().get()));

        if (thenList.isEmpty() || elseList.isEmpty()) {
            return List.of();
        }

        return Stream.concat(thenList.stream(), elseList.stream()).collect(Collectors.toList());
    }

    private void checkIfMainMethodParameter(Reference reference) {
        Optional<VariableDefinition> tempReference = nameResolution.definitions().getReference(reference);
        var mainMethodParameter = mainMethod.orElseThrow().getParameters().get(0);
        if (tempReference.isPresent() && tempReference.get() == mainMethodParameter)
            reportError(new MainMethodProblems.UsingArgs(reference));
    }

    @SuppressWarnings("DuplicateBranchesInSwitch")
    private void checkExpression(Expression expression) {
        switch (expression) {
            case MethodCallExpression methodCallExpression -> {
                //Checks if the main Method is called.
                var methodDef = nameResolution.definitions()
                        .getMethod(methodCallExpression)
                        .flatMap(m -> m instanceof DefinedMethod def ? Optional.of(def.getAstMethod()) : Optional.empty());

                if (methodDef.equals(mainMethod)) {
                    reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                }

                if (inMainMethod && methodCallExpression.getTarget().isEmpty()) {
                    reportError(new ImplicitThis.ImplicitThisMethodCall(methodCallExpression));
                }

                for (Expression expr : methodCallExpression.getArguments()) {
                    checkExpression(expr);
                }

                methodCallExpression.getTarget().ifPresent(this::checkExpression);
            }
            case AssignmentExpression assignmentExpression -> {
                Expression temp = assignmentExpression.getLvalue();
                if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                    checkExpression(assignmentExpression.getLvalue());
                    checkExpression(assignmentExpression.getRvalue());
                } else {
                    reportError(new AssignmentExpressionLeft(assignmentExpression));
                }
            }
            case ThisExpression thisExpression -> {
                if (inMainMethod) {
                    reportError(new MainMethodProblems.ReferenceUsingStatic(thisExpression));
                }
            }
            case BinaryOpExpression binaryOpExpression -> {
                checkExpression(binaryOpExpression.getLhs());
                checkExpression(binaryOpExpression.getRhs());
            }
            case UnaryExpression unaryExpression -> checkExpression(unaryExpression.getExpression());
            case FieldAccessExpression fieldAccessExpression -> checkExpression(fieldAccessExpression.getTarget());
            case ArrayAccessExpression arrayAccessExpression -> {
                checkExpression(arrayAccessExpression.getIndexExpression());
                checkExpression(arrayAccessExpression.getTarget());
            }
            case BoolLiteral ignored -> {
            }
            case IntLiteral ignored -> {
            }
            case NewObjectExpression newObjectExpression -> {
                var type = nameResolution.expressionTypes().get(newObjectExpression);

                if (type.isPresent() && type.get() instanceof ClassTy classTy && classTy instanceof IntrinsicClassTy) {
                    reportError(new NewObjectStringUse(newObjectExpression));
                }
            }
            case NewArrayExpression newArray -> checkExpression(newArray.getFirstDimensionSize());
            case NullExpression ignored -> {
            }
            case Reference reference -> {
                if (inMainMethod) {
                    Optional<VariableDefinition> tempField = nameResolution.definitions().getReference(reference);

                    if (tempField.map(def -> def instanceof Field).orElse(false))
                        reportError(new ImplicitThis.ImplicitThisFieldCall(reference, tempField.get()));

                    checkIfMainMethodParameter(reference);
                }
            }
        }
    }


    private void checkStatement(Statement statement) {
        switch (statement) {
            case Block block -> {
                for (Statement child : block.getStatements()) {
                    checkStatement(child);
                }
            }
            case IfStatement ifStatement -> {
                checkExpression(ifStatement.getCondition());

                checkStatement(ifStatement.getThenBody());

                if (ifStatement.getElseBody().isPresent())
                    checkStatement(ifStatement.getElseBody().get());
            }
            case WhileStatement whileStatement -> {
                checkExpression(whileStatement.getCondition());
                checkStatement(whileStatement.getBody());
            }
            case LocalVariableDeclarationStatement lclVrlStmt -> {
                this.countedLocalVariablesInCurrentMethod += 1;
                if (lclVrlStmt.getInitializer().isPresent())
                    checkExpression(lclVrlStmt.getInitializer().get());
            }
            case ExpressionStatement expressionStatement -> {
                var expression = expressionStatement.getExpression();
                if (expression instanceof MethodCallExpression || expression instanceof AssignmentExpression) {
                    checkExpression(expression);
                } else {
                    reportError(new WrongExpressionStatements(expression));
                }
            }
            case ReturnStatement returnStatement -> {
                if (returnStatement.getExpression().isPresent())
                    checkExpression(returnStatement.getExpression().get());
            }
            case EmptyStatement ignored -> {
            }
        }
    }

    private boolean isMainMethod(Method method) {
        return method.isStatic()
                && method.getReturnType() instanceof VoidType
                && method.getIdentifier().getContent().equals("main")
                && method.getParameters().size() == 1
                && method.getParameters().get(0).getType() instanceof ArrayType arrayType
                && arrayType.getChildType() instanceof ClassType classType
                && classType.getIdentifier().getContent().equals("String");
    }
}
