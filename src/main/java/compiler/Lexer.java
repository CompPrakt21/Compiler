package compiler;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

public class Lexer {

    private static final Map<String, TokenType> KEYWORDS_BY_REPR =
            TokenType.KEYWORDS.stream().collect(Collectors.toMap(k -> k.repr, k -> k));

    // Indexes all operators in two ways:
    // First by length, and then for each length by the concrete string repr.
    // This index allows for quickly matching the longest operator.
    private static final List<Map<String, TokenType>> INDEXED_OPERATORS;

    static {
        Map<Integer, Map<String, TokenType>> operatorsByLength = TokenType.OPERATORS.stream()
                .collect(Collectors.groupingBy(o -> o.repr.length(),
                        Collectors.toMap(o -> o.repr, o -> o)));
        int maxOperatorLength = operatorsByLength.keySet().stream().max(Integer::compareTo).get();
        INDEXED_OPERATORS = new ArrayList<>();
        for (int i = 0; i <= maxOperatorLength; i++) {
            Map<String, TokenType> ops = operatorsByLength.containsKey(i)
                    ? operatorsByLength.get(i)
                    : new HashMap<>();
            INDEXED_OPERATORS.add(ops);
        }
    }

    // The parser may sometimes want to add "synthetic" tokens to the lex stream for ease of use.
    private final ArrayDeque<Token> syntheticTokens = new ArrayDeque<>();
    private final String fileContent;
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

    private char peekAt(int i) {
        return fileContent.charAt(i);
    }

    private char peek() {
        return peekAt(currentPos);
    }

    private String lookahead(int n) {
        StringBuilder result = new StringBuilder();
        for (int i = currentPos; i < currentPos + n; i++) {
            if (isEOFAt(i)) {
                return result.toString();
            }
            result.append(peekAt(i));
        }
        return result.toString();
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

    private record ConsumedString(String text, Span span) {
    }

    private ConsumedString consumeWhile(BooleanSupplier predicate) {
        int startPos = currentPos;
        StringBuilder wordBuilder = new StringBuilder();
        while (!isEOF() && predicate.getAsBoolean()) {
            wordBuilder.append(peek());
            next();
        }
        return new ConsumedString(wordBuilder.toString(), new Span(startPos, currentPos - startPos));
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    private void consumeWhitespace() {
        consumeWhile(() -> isWhitespace(peek()));
    }

    private Optional<Span> consumeComment() {
        if (!expected("/*")) {
            return Optional.empty();
        }
        next(2);
        ConsumedString cs = consumeWhile(() -> !expected("*/"));
        // isEOF() = true => no end of comment symbol was found
        if (isEOF()) {
            return Optional.of(cs.span);
        }
        next(2);
        return Optional.empty();
    }

    private Optional<Span> consumeCommentsAndWhitespace() {
        int lastCurrentPos;
        do {
            lastCurrentPos = currentPos;
            Optional<Span> error = consumeComment();
            if (error.isPresent()) {
                return error;
            }
            consumeWhitespace();
        } while (lastCurrentPos != currentPos);
        return Optional.empty();
    }

    private static boolean isAsciiAlphabetic(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
    }

    private static boolean isAsciiNumeric(char c) {
        return c >= '0' && c <= '9';
    }

    private Optional<Token> consumeIntLiteral() {
        // 0x where x is a number [0-9]* is lexed as two literals 0 and x
        if (peek() == '0') {
            Optional<Token> t = Optional.of(Token.intLiteral(0, new Span(currentPos, 1)));
            next();
            return t;
        }
        ConsumedString cs = consumeWhile(() -> isAsciiNumeric(peek()));
        String word = cs.text;
        Span span = cs.span;
        if (word.length() == 0) {
            return Optional.empty();
        }
        return Optional.of(Token.intLiteral(word, span));
    }

    private Optional<Token> consumeKeywordOrIdent() {
        ConsumedString cs = consumeWhile(() -> {
            char c = peek();
            return isAsciiAlphabetic(c) || isAsciiNumeric(c) || c == '_';
        });
        String word = cs.text;
        Span span = cs.span;
        if (word.length() == 0) {
            return Optional.empty();
        }
        if (KEYWORDS_BY_REPR.containsKey(word)) {
            return Optional.of(Token.keyword(KEYWORDS_BY_REPR.get(word), span));
        }
        return Optional.of(Token.identifier(word, span));
    }

    private Token consumeOperator() {
        // We needn't worry about the start of comment symbol /* here.
        // consumeOperator() is always called when it's guaranteed that
        // at the current position there's neither whitespace nor a comment,
        // so the start of comment symbol certainly doesn't occur at currentPos.
        // OTOH, we will never accidentally match e.g. the beginning of /* (i.e. /)
        // as part of an operator, because there are no operators with / after the
        // first character.
        // If we were to extend the language with more operators, this part of the lexer
        // might need adjustment.
        for (int i = INDEXED_OPERATORS.size() - 1; i >= 0; i--) {
            Map<String, TokenType> ops = INDEXED_OPERATORS.get(i);
            String code = lookahead(i);
            if (ops.containsKey(code)) {
                Token result = Token.operator(ops.get(code), new Span(currentPos, i));
                next(i);
                return result;
            }
        }
        Token err = Token.error(this.fileContent.substring(currentPos, currentPos + 1), new Span(currentPos, 1));
        next();
        return err;
    }

    /**
     * Advances token stream.
     *
     * @return The next token.
     */
    public Token nextToken() {
        if (!syntheticTokens.isEmpty()) {
            return syntheticTokens.removeFirst();
        }
        Optional<Span> error = consumeCommentsAndWhitespace();
        if (error.isPresent()) {
            return Token.error("Missing closing `*/` for comment.", error.get());
        }
        if (isEOF()) {
            return Token.eof(new Span(currentPos, 1));
        }
        return consumeIntLiteral().or(this::consumeKeywordOrIdent).orElseGet(this::consumeOperator);
    }

    /**
     * Doesn't advance token stream.
     *
     * @return The next token.
     */
    public Token peekToken() {
        if (this.syntheticTokens.isEmpty()) {
            int startPos = currentPos;
            Token next = nextToken();
            // i wish all side effects allowed for time travel
            currentPos = startPos;
            return next;
        } else {
            return this.syntheticTokens.getFirst();
        }
    }

    public void addSyntheticToken(Token t) {
        syntheticTokens.add(t);
    }
}
