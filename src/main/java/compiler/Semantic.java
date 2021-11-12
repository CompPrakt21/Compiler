package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;

import java.lang.Class;
import java.util.*;

public class Semantic {

    private boolean foundMainMethod = false;
    private AstNode expectedReturnStatements;
    boolean correct = true;
    int expectReturn = 0;
    boolean isStatic = false;

    public boolean checkWellFormdness(AstNode node) {
        if(node == null) fail("Nothing?", node);
        if(node.getName() != "Program") fail("Starting nodes needs to be of type Program", node);
        if (node.isError()) fail("Error node found", node);
        List<AstNode> children = node.getChildren();
        foundMainMethod = false;
        recursiveCheckPerBlock(children);
        if (!foundMainMethod) fail("No main method was found", node);

        return correct;
        //TODO: Check if main method was created
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
                    if (methodCallExpression.getVariable().equals("main")) {
                        correct = false;
                        fail("Main method was called", methodCallExpression);
                    }
                    //TODO: check if Parameters are correct? Are references correct?
                    recursiveCheckPerBlock(children);
                    break;
                //Checks if a has already variable been instanciated or if its type is void
                case Field field:
                    if (instanciatedVars.contains(field.getVariable()) || children.get(0) instanceof VoidType) {
                        correct = false;
                        fail("Field was instanciated int this program already", field);
                    }
                    instanciatedVars.add(field.getVariable());
                    recursiveCheckPerBlock(children);
                    break;
                //Checks if a variable has already been instanciated in this block or if its type is of void
                case LocalVariableDeclarationStatement localVariableDeclarationStatement:
                    if (instanciatedVars.contains(localVariableDeclarationStatement.getVariable()) || children.get(0) instanceof  VoidType) {
                        correct = false;
                        fail("Var was instanciated in this block already or the var has type void", localVariableDeclarationStatement);
                    }
                    instanciatedVars.add(localVariableDeclarationStatement.getVariable());
                    recursiveCheckPerBlock(children);
                    break;
                //checks that only one static method exists and that that one is a correctly formed "main".
                //Checks what return type is expected
                case Method method:
                    if (method.getIsStatic() && method.getVariable().equals("main") && !foundMainMethod && checkMainMethod(method)) {
                        foundMainMethod = true;
                        isStatic = true;
                    }else if (method.getVariable().equals("main") || method.getIsStatic()) {
                        fail("Two main methods or two static methods were detected", method);
                        break;
                    }
                    if (instanciatedMethods.contains(method.getName())) fail("Method overloading is disallowed", method);
                    instanciatedMethods.add(method.getName());
                    expectReturn = (children.get(0) instanceof VoidType) ? 0 : 1;
                    recursiveCheckPerBlock(children);
                    if (expectReturn > 0) fail("Not all paths were covered by a return", method); //TODO
                    isStatic = false;
                    break;
                //Checks if the left side of the assignment is formed correctly. Check if type matches?
                case AssignmentExpression assignmentExpression:
                    AstNode temp = children.get(0);
                    if (temp instanceof Reference || temp instanceof ArrayAccessExpression || temp instanceof FieldAccessExpression) {
                        recursiveCheckPerBlock(children);
                        break;
                    }
                    fail("Wrong left side in assignment", assignmentExpression);
                    recursiveCheckPerBlock(children);
                    break;
                //Check all return paths
                case IfStatement ifStatement:
                    expectReturn += expectReturn > 0 ? ifStatement.getChildren().size() - 1 : 0;
                    recursiveCheckPerBlock(children);
                    if (expectReturn >= oldExpectReturn) expectReturn = oldExpectReturn;
                    break;
                //Check all return paths
                case WhileStatement whileStatement:
                    expectReturn += expectReturn > 0 ? 1 : 0;
                    recursiveCheckPerBlock(children);
                    if (expectReturn >= oldExpectReturn) expectReturn = oldExpectReturn;
                    break;
                //Checks if a return is expected and delivered
                case ReturnStatement returnStatement:
                    expectReturn -= expectReturn > 0 ? 1 : 0;
                    recursiveCheckPerBlock(children);
                    break;

                case ThisExpression thisExpression:
                    if (isStatic) fail("This call in static method " , thisExpression);
                    recursiveCheckPerBlock(children);
                    break;

                case null, default:
                    recursiveCheckPerBlock(children);;

            }
        }
        //Checks if everything worked
        return correct;
    }


    private boolean checkMainMethod(Method node){
        List<AstNode> children = node.getChildren();
        boolean test = true;
        test &= children.get(0) instanceof VoidType
                && node.getIsStatic()
                && children.get(1) instanceof Parameter
                && children.get(1).getChildren().get(0) instanceof ArrayType
                && children.get(1).getChildren().get(0).getChildren().get(0) instanceof ClassType
                && children.get(1).getChildren().get(0).getChildren().get(0).getVariable() == "String"
                && children.get(2) instanceof Block;

        return test;

    }

    private void fail(String errmsg, AstNode node) {//TODO
        correct = false;
        if (node != null) {
            System.out.println(errmsg + " : " + node.getName() + " : " + node.getSpan());
        } else {
            System.out.println(errmsg + " null node");
        }
    }



}
