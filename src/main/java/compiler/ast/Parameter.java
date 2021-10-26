package compiler.ast;

public final class Parameter extends AstNode {
    private Type type;
    private String identifier;

    public Parameter(Type type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Parameter other)) {
            return false;
        }
        return this.type.syntacticEq(other.type)
                && this.identifier.equals(other.identifier);
    }
}
