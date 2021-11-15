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

    private boolean foundMainMethod = false;
    private AstNode expectedReturnStatements;
    boolean correct = true;
    int expectReturn = 0;
    boolean isStatic = false;
    private final Optional<CompilerMessageReporter> reporter;
    private final NameResolution.NameResolutionResult nameResolution;

    public Semantic(CompilerMessageReporter reporter, NameResolution.NameResolutionResult nameResolution) {
        this.nameResolution = nameResolution;
        this.reporter = Optional.of(reporter);
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    public boolean checkWellFormdness(Program node) {
        if(node == null) return false;
        if(node.getName() != "Program") return false;
        if (node.isError()) return false;       //nodes cannot have error
        List<Class> children = node.getClasses();
        foundMainMethod = false;
        correct = checkClasses(children);
        if (!foundMainMethod) {
            reportError(new MainMethodProblems.MainMethodMissing());
            return false;
        };

        return correct;
    }

    private boolean checkExpressions(Expression expression) {

        return true;
    }

    private boolean checkClasses(List<Class> classExpression) {

        for (Class klass : classExpression) {
            recursiveCheckPerBlock(List.of(klass));
        }

        return true;
    }


    private boolean recursiveCheckPerBlock(List<AstNode> nodes) {
        //TODO: Check all children
        for (AstNode child: nodes) {
            switch (child) {
                //Checks if the main Method is called.
                case MethodCallExpression methodCallExpression:
                    if (methodCallExpression.getIdentifier().getContent().equals("main")) {
                        reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                        correct = false;
                    }
                    break;
                //checks that only one static method exists and that that one is a correctly formed "main".
                //Checks what return type is expected
                case Method method:
                    if (method.isStatic() && method.getName().equals("main") && !foundMainMethod && checkMainMethod(method)) {
                        foundMainMethod = true;
                        isStatic = true;
                    }else if (method.getIdentifier().equals("main") || method.isStatic()) {
                        reportError(new MainMethodProblems.MultipleStaticMethods(method));
                        correct = false;
                    }
                    expectReturn = (method.getReturnType() instanceof VoidType) ? 0 : 1;
                    correct &= recursiveCheckPerBlock(List.of(method.getBody()));
                    if (expectReturn > 0) {
                        reportError(new ReturnStatementErrors.MissingReturnOnPath(method));
                        correct = false;
                    }
                    isStatic = false;
                    break;
                //Checks if the left side of the assignment is formed correctly. Check if type matches?
                case AssignmentExpression assignmentExpression:
                    AstNode temp = assignmentExpression.getLvalue(); //TODO;
                    if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                        correct &= recursiveCheckPerBlock(List.of(assignmentExpression.getRvalue()));
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

                case null, default:
                    break;

            }
        }
        //Checks if everything worked
        return correct;
    }


    private boolean checkMainMethod(Method node){
        boolean test = node.getReturnType() instanceof VoidType
                && node.isStatic()
                && node.getParameters().size() == 1
                && node.getParameters().get(0).getType() instanceof ArrayType arrayType
                && arrayType.getChildType() instanceof ClassType classType
                && classType.getIdentifier().getContent().equals("String");

        return test;

    }


}
