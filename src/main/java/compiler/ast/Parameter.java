package compiler.ast;

public final class Parameter {
    private Type type;
    private String identifier;

    public Parameter(Type type, String identifier) {
        this.type = type;
        this.identifier = identifier;
    }
}
