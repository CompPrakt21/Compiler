package compiler.ast;

public final class Reference extends Expression {
    private String identifier;

    public Reference(String identifier) {
        this.identifier = identifier;
    }
}
