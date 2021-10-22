package compiler.ast;

public final class ClassType extends Type {
    private String identifier;

    public ClassType(String identifier) {
        this.identifier = identifier;
    }
}
