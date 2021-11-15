package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;
import compiler.resolution.NameResolution;

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

        return correct;
    }

    private boolean checkClasses(List<Class> classExpression) {

        for (Class klass : classExpression) {
            checkMethods(klass.getMethods());
        }

        return true;
    }

    private boolean checkMethods(List<Method> methods) {
        for (Method method : methods) {
            if (method.getIdentifier().getContent().equals("main")) {
                checkMainMethod(method);
                continue;
            }
            else {

            }
        }
        return true;

    }


    private boolean checkExpressions(List<Expression> nodes) {
        //TODO: Check all children
        for (Expression child : nodes) {
            switch (child) {
                //Checks if the main Method is called.
                case MethodCallExpression methodCallExpression:
                    if (methodCallExpression.getIdentifier().getContent().equals("main")) {
                        reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                        correct = false;
                    }
                    break;
                //Checks if the left side of the assignment is formed correctly. Check if type matches?
                case AssignmentExpression assignmentExpression:
                    AstNode temp = assignmentExpression.getLvalue();
                    if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                        correct &= checkExpressions(List.of(assignmentExpression.getRvalue()));
                        break;
                    }
                    reportError(new AssignmentExpressionLeft(assignmentExpression));
                    correct = false;
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
                && node.isStatic()
                && node.getParameters().size() == 1
                && node.getParameters().get(0).getType() instanceof ArrayType arrayType
                && arrayType.getChildType() instanceof ClassType classType
                && classType.getIdentifier().getContent().equals("String");

        if (!test) return false;
        mainMethod = Optional.of(node);
        return true;

    }


}
