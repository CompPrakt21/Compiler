package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.resolution.NameResolution;
import compiler.types.UnresolveableTy;

import java.lang.constant.ClassDesc;
import java.util.*;

public class Semantic {

    private Optional<Method> mainMethod;
    private AstNode expectedReturnStatements;
    boolean correct = true;
    int expectReturn = 0;
    boolean isStatic = false;
    private final Optional<CompilerMessageReporter> reporter;
    private final NameResolution.NameResolutionResult nameResolution;
    private List<MethodCallExpression> doLast = new ArrayList<>();

    public Semantic(CompilerMessageReporter reporter, NameResolution.NameResolutionResult nameResolution) {
        this.nameResolution = nameResolution;
        this.reporter = Optional.of(reporter);
        mainMethod = Optional.empty();
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    public boolean checkWellFormdness(Program node) {
        List<Class> children = node.getClasses();
        correct = checkClasses(children);

        if (mainMethod.isEmpty()) {
            reportError(new MainMethodProblems.MainMethodMissing());
            return false;
        }
        for (MethodCallExpression expr : doLast) {
            correct &= checkExpressions(expr);
        }

        return correct;
    }

    private boolean checkClasses(List<Class> classExpression) {

        for (Class klass : classExpression) {
            correct &= checkMethods(klass.getMethods());
        }

        return correct;
    }

    private boolean checkMethods(List<Method> methods) {
        for (Method method : methods) {
            if (method.isStatic() && !mainMethod.isEmpty()){
                reportError(new MainMethodProblems.MultipleStaticMethods(method));
                correct = false;
            }
            if (mainMethod.isEmpty() && method.isStatic()) {
                if (checkMainMethod(method))
                    isStatic = true;

            }

            checkStatements(method.getBody().getStatements());
            isStatic = false;
        }
        return correct;

    }

    private boolean checkFirstExpression(Expression expression) {
        switch (expression) {
            case MethodCallExpression d:
                return checkExpressions(expression);
            case AssignmentExpression ignored:
                return checkExpressions(expression);
            case default:
                reportError(new WrongExpressionStatements(expression));
                return false;

        }
    }

    private boolean checkParametersForArgs(List<Expression> expressions) {
        for (Expression expression : expressions) {
            if (expression instanceof Reference reference) {
                if(reference.getIdentifier().getContent().equals("args")){
                    reportError(new MainMethodProblems.UsingArgs(reference));
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkExpressions(Expression expression) {

        switch (expression) {
            //Checks if the main Method is called.
            case MethodCallExpression methodCallExpression:
                if (methodCallExpression.getIdentifier().getContent().equals("main")) {
                    if (mainMethod.isEmpty())
                        doLast.add(methodCallExpression);
                    else {
                        if (nameResolution.definitions().getMethod(methodCallExpression).equals(mainMethod)) {
                            reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                            correct = false;
                        }
                    }
                }
                if (isStatic) {
                    correct &= checkParametersForArgs(methodCallExpression.getArguments());
                }
                break;
            //Checks if the left side of the assignment is formed correctly. Check if type matches?
            case AssignmentExpression assignmentExpression:
                AstNode temp = assignmentExpression.getLvalue();
                if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                    correct &= checkExpressions(assignmentExpression.getRvalue());
                    break;
                }
                reportError(new AssignmentExpressionLeft(assignmentExpression));
                correct = false;
                break;

            case ThisExpression thisExpression:
                System.out.println(isStatic);
                if (isStatic) {
                    reportError(new MainMethodProblems.ReferenceUsingStatic(thisExpression));
                    correct = false;
                }
                break;
            case default, null:
                return correct;
        }
        return correct;
    }


    private boolean checkStatements(List<Statement> nodes) {
        //TODO: Check all children
        for (Statement child : nodes) {
            switch (child) {
                case Block block:
                    correct &= checkStatements(block.getStatements());
                    break;
                case IfStatement ifStatement:
                    if (isStatic) {
                        correct &= checkExpressions(ifStatement.getCondition());
                    }
                    correct &= checkStatements(List.of(ifStatement.getThenBody()));
                    if (!ifStatement.getElseBody().isEmpty())
                        correct &= checkStatements(List.of(ifStatement.getElseBody().get()));
                    break;
                case WhileStatement whileStatement:
                    if (isStatic)
                        correct &= checkExpressions(whileStatement.getCondition());
                    correct &= checkStatements(List.of(whileStatement.getBody()));
                    break;
                case LocalVariableDeclarationStatement lclVrlStmt:
                    if (isStatic && !lclVrlStmt.getInitializer().isEmpty())
                        correct &= checkExpressions(lclVrlStmt.getInitializer().get());
                    if(lclVrlStmt.getType() instanceof ClassType classType && classType.getIdentifier().getContent().equals("String")) {
                        correct = false;
                        reportError(new LocalDeclarationErrors.StringUsed(lclVrlStmt));
                        break;
                    }
                    break;
                case ExpressionStatement expressionStatement:
                    correct &= checkFirstExpression(expressionStatement.getExpression());
                    break;

                case null, default:
                    break;

            }
        }
        //Checks if everything worked
        return correct;
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
        return true;

    }


}
