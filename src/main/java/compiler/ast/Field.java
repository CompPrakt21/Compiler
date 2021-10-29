package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public class Field extends AstNode {
    private String identifier;

    private Type type;

    public Field(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Field other)) {
            return false;
        }
        return this.identifier.equals(other.identifier) && this.type.syntacticEq(other.type);
    }
}
