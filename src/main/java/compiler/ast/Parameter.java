package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class Parameter extends AstNode{
    private Type type;
    private String identifier;

    public Parameter(Type type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
    }
}
