package compiler;

import java.util.List;

public class MockLexer {
    private List<Token> tokens;
    private int next;

    public MockLexer(List<Token> tokens) {
        this.tokens = tokens;
        this.next = 0;
    }

    /**
     * Advances token stream.
     *
     * @return The next token.
     */
    public Token nextToken() {
        this.next += 1;
        return this.tokens.get(this.next - 1);
    }

    /**
     * Doesn't advance token stream.
     *
     * @return The next token.
     */
    public Token peekToken() {
        return this.tokens.get(this.next);
    }
}
