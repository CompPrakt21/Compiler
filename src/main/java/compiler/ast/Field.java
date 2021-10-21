package compiler.ast;

public class Field extends AstNode {
    private String identifier;

    private Type type;

    public Field(String identifier, Type type) {
        this.identifier = identifier;
        this.type = type;
    }
}
