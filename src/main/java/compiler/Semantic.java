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



}
