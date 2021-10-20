package compiler;

public class Lexer {

    private String fileContent;

    public Lexer(String fileContent) {
        this.fileContent = fileContent;
    }

    /**
     * Advances token stream.
     * @return The next token.
     */
    public Token nextToken() {
        return null;
    }

    /**
     * Doesn't advance token stream.
     * @return The next token.
     */
    public Token peekToken() {
        return null;
    }
}
