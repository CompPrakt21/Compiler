package compiler.ast;

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
}
