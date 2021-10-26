package compiler.ast;

public final class ClassType extends Type {
    private String identifier;

    public ClassType(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ClassType other)) {
            return false;
        }
        return this.identifier.equals(other.identifier);
    }
}
