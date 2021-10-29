package compiler.ast;

public class Field extends AstNode {
    private String identifier;

    private Type type;

    public Field(String identifier, Type type) {
        this.isError |= identifier == null || type == null;

        this.identifier = identifier;
        this.type = type;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Field other)) {
            return false;
        }
        return this.identifier.equals(other.identifier) && this.type.syntacticEq(other.type);
    }
}
