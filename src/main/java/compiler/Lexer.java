package compiler;

import java.util.*;
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
        for (int i = currentPos; i < currentPos+n; i++) {
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

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }

    private void consumeWhitespace() {
        while (!isEOF() && isWhitespace(peek())) {
            next();
        }
    }

    private Optional<Span> consumeComment() {
        if (!expected("/*")) {
            return Optional.empty();
        }
        int startPos = currentPos;
        next(2);
        while (!isEOF() && !expected("*/")) {
            next();
        }
        // isEOF() = true => no end of comment symbol was found
        if (isEOF()) {
            return Optional.of(new Span(startPos, currentPos-startPos));
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

    private Optional<Token> consumeAlphanumericWord() {
        int startPos = currentPos;
        StringBuilder wordBuilder = new StringBuilder();
        char c = peek();
        while (!isEOF() && (isAsciiAlphabetic(c) || isAsciiNumeric(c) || c == '_')) {
            wordBuilder.append(c);
            next();
            c = peek();
        }
        String word = wordBuilder.toString();
        if (word.length() == 0) {
            return Optional.empty();
        }
        Span span = new Span(startPos, currentPos-startPos);
        if (KEYWORDS_BY_REPR.containsKey(word)) {
            return Optional.of(Token.keyword(KEYWORDS_BY_REPR.get(word), span));
        }
        if (word.chars().allMatch(Character::isDigit)) {
            Optional<Token> error = Optional.of(Token.error("Integer literal value too large.", span));
            try {
                long value = Long.parseLong(word);
                if (value > -((long) Integer.MIN_VALUE)) {
                    return error;
                }
                return Optional.of(Token.intLiteral(value, span));
            } catch (NumberFormatException e) {
                // Value too large for long
                return error;
            }
        }
        if (isAsciiAlphabetic(word.charAt(0)) || word.charAt(0) == '_') {
            return Optional.of(Token.identifier(word, span));
        }
        return Optional.of(Token.error("Invalid identifier name. First character needs to be alphabetic.", span));
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
        for (int i = INDEXED_OPERATORS.size()-1; i >= 0; i--) {
            Map<String, TokenType> ops = INDEXED_OPERATORS.get(i);
            String code = lookahead(i);
            if (ops.containsKey(code)) {
                Token result = Token.operator(ops.get(code), new Span(currentPos, i));
                next(i);
                return result;
            }
        }
        Token err = Token.error("Invalid symbol.", new Span(currentPos, 1));
        next();
        return err;
    }

    /**
     * Advances token stream.
     *
     * @return The next token.
     */
    public Token nextToken() {
        Optional<Span> error = consumeCommentsAndWhitespace();
        if (error.isPresent()) {
            return Token.error("Missing closing `*/` for comment.", error.get());
        }
        if (isEOF()) {
            return Token.eof(new Span(currentPos, 1));
        }
        Optional<Token> token = consumeAlphanumericWord();
        if (token.isPresent()) {
            return token.get();
        }
        return consumeOperator();
    }

    /**
     * Doesn't advance token stream.
     *
     * @return The next token.
     */
    public Token peekToken() {
        int startPos = currentPos;
        Token next = nextToken();
        // i wish all side effects allowed for time travel
        currentPos = startPos;
        return next;
    }
}
