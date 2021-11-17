package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.resolution.NameResolution;
import compiler.types.Ty;
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

    private boolean checkReturnPathsInStatements(List<Statement> statements) {
        for (Statement statement : statements) {
            if (statement instanceof ReturnStatement) return true;
            if (statement instanceof IfStatement ifstmt) return hasReturnInIfElse(ifstmt);

        }
        return false;
    }

    private boolean hasReturnInIfElse(IfStatement ifStatement){
        if (ifStatement.getElseBody().isEmpty()) return false;
        return checkReturnPathsInStatements(List.of(ifStatement.getThenBody())) && checkReturnPathsInStatements(List.of(ifStatement.getElseBody().get()));

    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
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
            if (method.isStatic() && !mainMethod.isEmpty()){
                reportError(new MainMethodProblems.MultipleStaticMethods(method));
                correct = false;
            }
            if (mainMethod.isEmpty() && method.isStatic()) {
                if (checkMainMethod(method))
                    isStatic = true;

            }
            if (!(method.getReturnType() instanceof VoidType)) {
                correct &= checkReturnPathsInStatements(method.getBody().getStatements());
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
            correct = false;
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

    private void checkExpressions(Expression expression) {
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
                    checkExpressions(assignmentExpression.getLvalue());
                    checkExpressions(assignmentExpression.getRvalue());
                    break;
                }
                reportError(new AssignmentExpressionLeft(assignmentExpression));
                correct = false;
                break;

            case ThisExpression thisExpression:
                if (isStatic) {
                    reportError(new MainMethodProblems.ReferenceUsingStatic(thisExpression));
                    correct = false;
                }
                break;
            case BinaryOpExpression binaryOpExpression:
                checkExpressions(binaryOpExpression.getLhs());
                checkExpressions(binaryOpExpression.getRhs());
                break;
            case UnaryExpression unaryExpression:
                checkExpressions(unaryExpression.getExpression());
                break;
            case FieldAccessExpression fieldAccessExpression:
                checkExpressions(fieldAccessExpression.getTarget());
                break;
            case ArrayAccessExpression arrayAccessExpression:
                checkExpressions(arrayAccessExpression.getIndexExpression());
                checkExpressions(arrayAccessExpression.getTarget());
                break;


            case default, null:
                break;
        }
    }


    private void checkStatements(List<Statement> nodes) {
        //TODO: Check all children
        for (Statement child : nodes) {
            switch (child) {
                case Block block:
                    checkStatements(block.getStatements());
                    break;
                case IfStatement ifStatement:
                    if (isStatic) {
                        checkExpressions(ifStatement.getCondition());
                    }
                    checkStatements(List.of(ifStatement.getThenBody()));
                    if (!ifStatement.getElseBody().isEmpty())
                        checkStatements(List.of(ifStatement.getElseBody().get()));
                    break;
                case WhileStatement whileStatement:
                    if (isStatic)
                        checkExpressions(whileStatement.getCondition());
                    checkStatements(List.of(whileStatement.getBody()));
                    break;
                case LocalVariableDeclarationStatement lclVrlStmt:
                    if (isStatic && !lclVrlStmt.getInitializer().isEmpty())
                        checkExpressions(lclVrlStmt.getInitializer().get());
                    if(lclVrlStmt.getType() instanceof ClassType classType && classType.getIdentifier().getContent().equals("String")) {
                        correct = false;
                        reportError(new LocalDeclarationErrors.StringUsed(lclVrlStmt));
                        break;
                    }
                    break;
                case ExpressionStatement expressionStatement:
                    checkFirstExpression(expressionStatement.getExpression());
                    break;
                case ReturnStatement returnStatement:
                    if (!returnStatement.getExpression().isEmpty())
                        checkExpressions(returnStatement.getExpression().get());

                case null, default:
                    break;

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
        return true;

    }


}
