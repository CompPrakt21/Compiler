package compiler;

public class Token {

    private TokenType type;

    private Content content;

    public Span span;

    private sealed class Content permits IdentifierContent, IntLiteralContent {}

    private final class IdentifierContent extends Content {
        private String content;
    }

    private final class IntLiteralContent extends Content{
        private int content;
    }

    public String getIdentContent() {
        assert this.type == TokenType.Identifier;
        assert this.content instanceof IdentifierContent;

        return ((IdentifierContent) this.content).content;
    }

    public int getIntLiteralContent() {
        assert this.type == TokenType.IntLiteral;
        assert this.content instanceof IntLiteralContent;

        return ((IntLiteralContent) this.content).content;
    }
}