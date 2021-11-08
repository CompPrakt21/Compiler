package compiler;

import com.sun.source.util.DocSourcePositions;
import compiler.ast.*;

import java.lang.Class;
import java.util.*;

public class Semantic {

    private record Pair<K, V>(K first, V second) { }

    private HashMap<String, AstNode> stringMap = new HashMap<>();

    private int depth = 0;

    private final String BLOCKDIVIDER = "BLOCKDIVIDER";

    private Stack<Pair<String, AstNode>> semanticMap = new Stack<>();

    private boolean foundMainMethod = false;

    public Semantic() {

    }

    public void semanticAnalysis(AstNode node) {

        List<AstNode> children = node.getChildren();
        boolean newBlock = node.startsNewBlock();
        if (newBlock) {
            semanticMap.push(new Pair<>(BLOCKDIVIDER, null));
        }

        String variable = node.getVariable();
        if (node instanceof Reference) {
            if (stringMap.get(variable) == null) {
                node.makeError(true);
            }
            ((Reference) node).setReference(stringMap.get(variable));
            return;
        }
        if (variable != null) {
            stringMap.put(variable, node);
            semanticMap.push(new Pair(variable, node)); //is it possible to initialize a var multiple times?
        }
        if (children != null) {
            for (AstNode child : children) {
                semanticAnalysis(child);
            }
        }
        if (newBlock) {
            Pair<String, AstNode> old = semanticMap.pop();
            List<String> removed = new ArrayList<>();
            while (!old.first.equals(BLOCKDIVIDER)) {
                removed.add(old.first);
                old = semanticMap.pop();
            }
            update(removed);

        }
    }

    private void update (List<String> removed) {
        ListIterator<Pair<String, AstNode>> iterator = semanticMap.listIterator(semanticMap.size());
        while (iterator.hasPrevious()) {
            Pair<String, AstNode> pair = iterator.previous();
            if (removed.contains(pair.first)) {
                stringMap.put(pair.first, pair.second);
                removed.remove(pair.first);
            }
        }
        for (int i = 0; i < removed.size(); i++) {
            stringMap.remove(removed.get(i));
        }

    }

    public void checkCorrectness(AstNode node) {
        if(node == null) fail();
        if(node.getName() != "Program") fail();
        if (node.isError()) fail();
        List<AstNode> children = node.getChildren();
        foundMainMethod = false;
        recursiveCheckPerBlock(children);


        boolean multipleInstanceInstanciations;
        boolean multipleMainMethods;
        boolean callsMain;
        boolean wrongAccess;
        boolean stringUsed;
        boolean correctMain;


    }

    private boolean recursiveCheckPerBlock(List<AstNode> node) {
        boolean correct = true;
        for (AstNode child: node) {
            List<AstNode> children = child.getChildren();
            ArrayList<String> instanciatedVars = new ArrayList<>();
            switch (child) {
                case MethodCallExpression methodCallExpression: if (methodCallExpression.getVariable() == "main"){ correct = false; fail();} break;
                case Field field: if (instanciatedVars.contains(field.getVariable())) {correct = false;fail();}; instanciatedVars.add(field.getVariable()); break;
                case LocalVariableDeclarationStatement localVariableDeclarationStatement: if (instanciatedVars.contains(localVariableDeclarationStatement.getVariable())) {correct = false;fail();} instanciatedVars.add(localVariableDeclarationStatement.getVariable()); break;
                case Block block: recursiveCheckPerBlock(children); break;
                case Method method: if (method.getIsStatic() && !foundMainMethod && checkMainMethod(method)) foundMainMethod = true; else {correct = false;fail();} break;
                case null, default: continue;

            }
        }
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

    private void fail() {

    }



}
