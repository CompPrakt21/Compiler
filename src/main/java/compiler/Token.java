package compiler;

public class Token {

    public TokenType type;
    private Content content;
    public Span span;

    private Token(TokenType type, Content content, Span span) {
        this.type = type;
        this.content = content;
        this.span = span;
    }

    public static Token identifier(String name, Span span) {
        return new Token(TokenType.Identifier, new IdentifierContent(name), span);
    }

    public static Token intLiteral(int value, Span span) {
        return new Token(TokenType.IntLiteral, new IntLiteralContent(value), span);
    }

    public static Token error(String errorMessage, Span span) {
        return new Token(TokenType.Error, new ErrorContent(errorMessage), span);
    }

    public static Token eof(Span span) {
        return new Token(TokenType.EOF, null, span);
    }

    public static Token keyword(TokenType type, Span span) {
        return new Token(type, null, span);
    }

    public static Token operator(TokenType type, Span span) {
        return new Token(type, null, span);
    }

    private static sealed class Content permits IdentifierContent, IntLiteralContent, ErrorContent {}

    private static final class IdentifierContent extends Content {
        private String content;
        private IdentifierContent(String content) { this.content = content; }
    }

    private static final class IntLiteralContent extends Content {
        private int content;
        private IntLiteralContent(int content) { this.content = content; }
    }

    private static final class ErrorContent extends Content {
        private String errorMessage;
        private ErrorContent(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public String getIdentContent() {
        assert this.type == TokenType.Identifier;
        assert this.content != null;
        assert this.content instanceof IdentifierContent;

        return ((IdentifierContent) this.content).content;
    }

    public int getIntLiteralContent() {
        assert this.type == TokenType.IntLiteral;
        assert this.content != null;
        assert this.content instanceof IntLiteralContent;

        return ((IntLiteralContent) this.content).content;
    }

    public String getErrorContent() {
        assert this.type == TokenType.Error;
        assert this.content != null;
        assert this.content instanceof ErrorContent;

        return ((ErrorContent) this.content).errorMessage;
    }
}