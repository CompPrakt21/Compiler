package compiler;

public class Lexer {

    // Used to signal EOF by peek() and next(). Bubbles up to nextToken(),
    // where the EOF token is emitted. This saves us the effort
    // of explicitly handling and forwarding EOF at every layer of the lexer.
    private static class EOF extends Exception { }

    private String fileContent;
    private int currentPos = 0;

    public Lexer(String fileContent) {
        this.fileContent = fileContent;
    }

    private boolean isEOFAt(int pos) {
        return pos >= fileContent.length();
    }

    private boolean isEOF() {
        return isEOFAt(currentPos);
    }

    private void checkEOF() throws EOF {
        if (isEOF()) {
            throw new EOF();
        }
    }

    private char peekAt(int i) {
        return fileContent.charAt(i);
    }

    private char peek() {
        return peekAt(currentPos);
    }

    private String lookahead(int n) {
        String result = "";
        for (int i = currentPos; i < currentPos+n; i++) {
            if (isEOFAt(i)) {
                return result;
            }
            result += peekAt(i);
        }
        return result;
    }

    private void next(int n) {
        currentPos += n;
    }

    private void next() {
        next(1);
    }

    private boolean expected(String expected) {
        String actual = lookahead(expected.length());
        return actual.equals(expected);
    }

    private void consumeWhitespace() {
        while (!isEOF() && Character.isWhitespace(peek())) {
            next();
        }
    }

    private boolean consumeComment() {
        if (!expected("/*")) {
            return false;
        }
        next(2);
        while (!isEOF() && !expected("*/")) {
            next();
        }
        // isEOF() = true => no end of comment symbol was found
        if (isEOF()) {
            return true;
        }
        next(2);
        return false;
    }

    private boolean consumeCommentsAndWhitespace() {
        int lastCurrentPos;
        do {
            lastCurrentPos = currentPos;
            boolean error = consumeComment();
            if (error) {
                return true;
            }
            consumeWhitespace();
        } while (lastCurrentPos != currentPos);
        return false;
    }

    /**
     * Advances token stream.
     * @return The next token.
     */
    public Token nextToken() {
        boolean error = consumeCommentsAndWhitespace();
        if (error) {
            // TODO: emit error token
        }
        if (isEOF()) {
            // TODO: emit EOF token
        }
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
