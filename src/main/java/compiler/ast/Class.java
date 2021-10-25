package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class Class extends AstNode {
    private String identifier;

    private List<Field> fields;
    private List<Method> methods;

    public Class(String identifier, List<Field> fields, List<Method> methods) {
        this.identifier = identifier;
        this.fields = fields;
        this.methods = methods;
    }


    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.addAll(fields);
        temp.addAll(methods);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
    }
}
