package compiler;

public class Token {

    private TokenType type;

    private Content content;

    public Span span;

    private sealed class Content permits IdentifierContent, IntLiteralContent, ErrorContent {}

    private final class IdentifierContent extends Content {
        private String content;
    }

    private final class IntLiteralContent extends Content {
        private int content;
    }

    private final class ErrorContent extends Content {
        private String errorMessage;
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

    public String getErrorContent() {
        assert this.type == TokenType.Error;
        assert this.content instanceof ErrorContent;

        return ((ErrorContent) this.content).errorMessage;
    }
}