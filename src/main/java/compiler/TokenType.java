package compiler;

public enum TokenType {
    EOF("<EOF>"),
    Abstract("abstract"),
    Assert("assert"),
    Boolean("boolean"),
    Break("break"),
    Byte("byte"),
    Case("case"),
    Identifier("<IDENT>"),
    IntLiteral("<INT>");

    // TODO: Add remaining TokenTypes.

    private String repr;

    TokenType(String repr) {
        this.repr = repr;
    }
}
