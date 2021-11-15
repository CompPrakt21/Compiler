package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;
import compiler.diagnostics.CompilerMessage;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.errors.*;

import java.lang.Class;
import java.util.*;

public class Semantic {

    private boolean foundMainMethod = false;
    private AstNode expectedReturnStatements;
    boolean correct = true;
    int expectReturn = 0;
    boolean isStatic = false;
    private final Optional<CompilerMessageReporter> reporter;

    public Semantic(CompilerMessageReporter reporter) {
        this.reporter = Optional.of(reporter);
    }

    private void reportError(CompilerMessage msg) {
        this.reporter.ifPresent(compilerMessageReporter -> compilerMessageReporter.reportMessage(msg));
    }

    public boolean checkWellFormdness(AstNode node) {
        if(node == null) return false;
        if(node.getName() != "Program") return false;
        if (node.isError()) return false;
        List<AstNode> children = node.getChildren();
        foundMainMethod = false;
        correct = recursiveCheckPerBlock(children);
        if (!foundMainMethod) {
            reportError(new MainMethodProblems.MainMethodMissing());
            return false;
        };

        return correct;
    }

    private boolean recursiveCheckPerBlock(List<AstNode> nodes) {
        //TODO: Check all children
        int oldExpectReturn = expectReturn;
        ArrayList<String> instanciatedVars = new ArrayList<>();
        ArrayList<String> instanciatedMethods = new ArrayList<>();
        if (nodes == null) {
            return true;
        }
        for (AstNode child: nodes) {
            List<AstNode> children = child.getChildren();
            switch (child) {
                //Checks if the main Method is called.
                case MethodCallExpression methodCallExpression:
                    if (methodCallExpression.getName().equals("main")) {
                        reportError(new MainMethodProblems.MainMethodCalled(methodCallExpression));
                        correct = false;
                    }
                    correct &= recursiveCheckPerBlock(children);
                    break;
                //Checks if a has already variable been instanciated or if its type is void
                case Field field:
                    if (instanciatedVars.contains(field.getName()) || children.get(0) instanceof VoidType) {
                        reportError(new MultipleUseOfSameMemberName(field, field));
                        correct = false;
                    }
                    instanciatedVars.add(field.getName());
                    correct &= recursiveCheckPerBlock(children);
                    break;
                //Checks if a variable has already been instanciated in this block or if its type is of void
                case LocalVariableDeclarationStatement localVariableDeclarationStatement:
                    if (instanciatedVars.contains(localVariableDeclarationStatement.getName()) || children.get(0) instanceof  VoidType) {
                        reportError(new LocalDeclarationErrors.MultipleUseOfSameVariableName(localVariableDeclarationStatement));
                        correct = false;
                    }
                    instanciatedVars.add(localVariableDeclarationStatement.getName());
                    correct &= recursiveCheckPerBlock(children);
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
                    if (instanciatedMethods.contains(method.getName())) {
                        reportError(new MultipleUseOfSameMemberName(method, method));
                        correct = false;
                    }
                    instanciatedMethods.add(method.getName());
                    expectReturn = (children.get(0) instanceof VoidType) ? 0 : 1;
                    correct &= recursiveCheckPerBlock(children);
                    if (expectReturn > 0) {
                        reportError(new ReturnStatementErrors.MissingReturnOnPath(method));
                        correct = false;
                    }
                    isStatic = false;
                    break;
                //Checks if the left side of the assignment is formed correctly. Check if type matches?
                case AssignmentExpression assignmentExpression:
                    AstNode temp = children.get(0);
                    if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                        correct &= recursiveCheckPerBlock(children);
                        break;
                    }
                    reportError(new AssignmentExpressionLeft(assignmentExpression));
                    correct = false;
                    break;
                //Check all return paths
                case IfStatement ifStatement:
                    expectReturn += expectReturn > 0 ? ifStatement.getChildren().size() - 1 : 0;
                    correct &= recursiveCheckPerBlock(children);
                    if (expectReturn >= oldExpectReturn) expectReturn = oldExpectReturn;
                    break;
                //Check all return paths
                case WhileStatement whileStatement:
                    expectReturn += expectReturn > 0 ? 1 : 0;
                    correct &= recursiveCheckPerBlock(children);
                    if (expectReturn >= oldExpectReturn) expectReturn = oldExpectReturn;
                    break;
                //Checks if a return is expected and delivered
                case ReturnStatement returnStatement:
                    expectReturn -= expectReturn > 0 ? 1 : 0;
                    correct &= recursiveCheckPerBlock(children);
                    break;

                case ThisExpression thisExpression:
                    if (isStatic) {
                        reportError(new MainMethodProblems.ReferenceUsingStatic(thisExpression));
                        correct = false;
                    }
                    correct &= recursiveCheckPerBlock(children);
                    break;

                case null, default:
                    correct &= recursiveCheckPerBlock(children);;

            }
        }
        //Checks if everything worked
        return correct;
    }


    private boolean checkMainMethod(Method node){
        List<AstNode> children = node.getChildren();
        boolean test = children.get(0) instanceof VoidType
                && node.isStatic()
                && children.get(1) instanceof Parameter
                && children.get(1).getChildren().get(0) instanceof ArrayType
                && children.get(1).getChildren().get(0).getChildren().get(0) instanceof ClassType
                && children.get(2) instanceof Block;

        return test;

    }


}
