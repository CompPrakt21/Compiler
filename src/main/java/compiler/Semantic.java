package compiler;

import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.resolution.DefinedMethod;
import compiler.resolution.MethodDefinition;
import compiler.resolution.NameResolution;

import java.util.*;

public class Semantic {

    private Optional<Method> mainMethod;
    private AstNode expectedReturnStatements;
    boolean correct = true;
    int expectReturn = 0;
    boolean isStatic = false;
    private Parameter mainMethodParam;
    private final Optional<CompilerMessageReporter> reporter;
    private final NameResolution.NameResolutionResult nameResolution;
    private List<MethodCallExpression> doLast = new ArrayList<>();

    public Semantic(CompilerMessageReporter reporter, NameResolution.NameResolutionResult nameResolution) {
        this.nameResolution = nameResolution;
        this.reporter = Optional.of(reporter);
        mainMethod = Optional.empty();
    }

    private boolean checkReturnPathsInStatements(List<Statement> statements) {
        boolean hasReturn = false;
        for (Statement statement : statements) {
            if (statement instanceof Block block) hasReturn |= checkReturnPathsInStatements(block.getStatements());
            if (statement instanceof ReturnStatement) hasReturn = true;
            if (statement instanceof IfStatement ifstmt && hasReturnInIfElse(ifstmt)) hasReturn = true;

        }
        return hasReturn;
    }

    private boolean hasReturnInIfElse(IfStatement ifStatement){
        if (ifStatement.getElseBody().isEmpty()) return false;
        return checkReturnPathsInStatements(List.of(ifStatement.getThenBody())) && checkReturnPathsInStatements(List.of(ifStatement.getElseBody().get()));

    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
        correct = false;
    }

    public boolean checkWellFormdness(Program node) {
        List<Class> children = node.getClasses();
        checkClasses(children);

        if (mainMethod.isEmpty()) {
            reportError(new MainMethodProblems.MainMethodMissing());
            return false;
        }
        for (MethodCallExpression expr : doLast) {
            checkExpressions(expr);
        }

        return correct;
    }

    private void checkClasses(List<Class> classExpression) {

        for (Class klass : classExpression) {
            checkMethods(klass.getMethods());
        }
    }

    private void checkMethods(List<Method> methods) {
        for (Method method : methods) {
            if (method.isStatic() && mainMethod.isPresent()){
                reportError(new MainMethodProblems.MultipleStaticMethods(method));
            }
            if (mainMethod.isEmpty() && method.isStatic()) {
                if (checkMainMethod(method))
                    isStatic = true;

            }
            if (!(method.getReturnType() instanceof VoidType)) {
                if (!checkReturnPathsInStatements(method.getBody().getStatements())) {
                    reportError(new ReturnStatementErrors.MissingReturnOnPath(method));
                }
            }
            checkStatements(method.getBody().getStatements());
            isStatic = false;
        }
    }

    private void checkFirstExpression(Expression expression) {
        if (expression instanceof MethodCallExpression || expression instanceof AssignmentExpression) {
            checkExpressions(expression);
        }else {
            reportError(new WrongExpressionStatements(expression));
        }
    }

    private void checkParametersForArgs(List<Expression> expressions) {
        for (Expression expression : expressions) {
            if (expression instanceof Reference reference) {
                Optional<VariableDefinition> tempReference = nameResolution.definitions().getReference(reference);
                if(tempReference.isPresent() && tempReference.get() == mainMethodParam)
                    reportError(new MainMethodProblems.UsingArgs(reference));

            }
        }
    }

    private void checkExpressions(Expression expression) {
        switch (expression) {
            //Checks if the main Method is called.
            case MethodCallExpression methodCallExpression -> {
                if (methodCallExpression.getIdentifier().getContent().equals("main")) {
                    if (mainMethod.isEmpty())
                        doLast.add(methodCallExpression);
                    else {
                        Optional<MethodDefinition> med = nameResolution.definitions().getMethod(methodCallExpression);
                        if (med.isPresent() && med.get() instanceof DefinedMethod definedMethod && definedMethod.getAstMethod().equals(mainMethod.get())) {
                            reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                        }
                    }
                }
                if (isStatic) {
                    checkParametersForArgs(methodCallExpression.getArguments());
                }
            }
            //Checks if the left side of the assignment is formed correctly. Check if type matches?
            case AssignmentExpression assignmentExpression -> {
                AstNode temp = assignmentExpression.getLvalue();
                if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                    checkExpressions(assignmentExpression.getLvalue());
                    checkExpressions(assignmentExpression.getRvalue());
                    break;
                }
                reportError(new AssignmentExpressionLeft(assignmentExpression));
            }

            case ThisExpression thisExpression -> {
                if (isStatic) {
                    reportError(new MainMethodProblems.ReferenceUsingStatic(thisExpression));
                }
            }
            case BinaryOpExpression binaryOpExpression -> {
                checkExpressions(binaryOpExpression.getLhs());
                checkExpressions(binaryOpExpression.getRhs());
            }
            case UnaryExpression unaryExpression -> {
                checkExpressions(unaryExpression.getExpression());
            }
            case FieldAccessExpression fieldAccessExpression -> {
                checkExpressions(fieldAccessExpression.getTarget());
            }
            case ArrayAccessExpression arrayAccessExpression -> {
                checkExpressions(arrayAccessExpression.getIndexExpression());
                checkExpressions(arrayAccessExpression.getTarget());
            }
            case BoolLiteral boolLiteral -> {}
            case IntLiteral intLiteral -> {}
            case NewObjectExpression newObjectExpression -> {
                if (newObjectExpression.getType().getIdentifier().getContent().equals("String")) {
                    reportError(new NewObjectStringUse(newObjectExpression));
                    break;
                }
            }
            case NewArrayExpression newArrayExpression -> {}
            case NullExpression nullExpression -> {}
            case Reference reference -> {}
        }
    }


    private void checkStatements(List<Statement> nodes) {
        //TODO: Check all children
        for (Statement child : nodes) {
            switch (child) {
                case Block block -> {
                    checkStatements(block.getStatements());
                }
                case IfStatement ifStatement -> {
                    if (isStatic) {
                        checkExpressions(ifStatement.getCondition());
                    }
                    checkStatements(List.of(ifStatement.getThenBody()));
                    if (!ifStatement.getElseBody().isEmpty())
                        checkStatements(List.of(ifStatement.getElseBody().get()));
                }
                case WhileStatement whileStatement -> {
                    if (isStatic)
                        checkExpressions(whileStatement.getCondition());
                    checkStatements(List.of(whileStatement.getBody()));
                }
                case LocalVariableDeclarationStatement lclVrlStmt -> {
                    if (isStatic && !lclVrlStmt.getInitializer().isEmpty())
                        checkExpressions(lclVrlStmt.getInitializer().get());
                }
                case ExpressionStatement expressionStatement -> {
                    checkFirstExpression(expressionStatement.getExpression());
                }
                case ReturnStatement returnStatement -> {
                    if (!returnStatement.getExpression().isEmpty())
                        checkExpressions(returnStatement.getExpression().get());
                }
                case EmptyStatement emptyStatement -> {}


            }
        }
        //Checks if everything worked
    }


    private boolean checkMainMethod(Method node) {
        boolean test = node.getReturnType() instanceof VoidType
                && node.getIdentifier().getContent().equals("main")
                && node.getParameters().size() == 1
                && node.getParameters().get(0).getType() instanceof ArrayType arrayType
                && arrayType.getChildType() instanceof ClassType classType
                && classType.getIdentifier().getContent().equals("String");

        if (!test) return false;
        mainMethod = Optional.of(node);
        mainMethodParam = node.getParameters().get(0);
        return true;

    }


}
