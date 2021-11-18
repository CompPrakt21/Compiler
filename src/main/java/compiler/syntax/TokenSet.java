package compiler.syntax;

import java.util.EnumSet;
import java.util.stream.Stream;

@SuppressWarnings("ClassCanBeRecord")
public class TokenSet implements TokenSetLike {

    private final EnumSet<TokenType> set;

    TokenSet(EnumSet<TokenType> set) {
        this.set = set;
    }

    public static TokenSet empty() {
        return new TokenSet(EnumSet.noneOf(TokenType.class));
    }

    public static TokenSet of(TokenSetLike... tokens) {
        var result = new TokenSet(EnumSet.noneOf(TokenType.class));

        for (var t : tokens) {
            t.addToTokenSet(result);
        }

        return result;
    }

    public Stream<TokenType> stream() {
        return this.set.stream();
    }

    public TokenSet add(TokenSetLike... others) {
        var res = new TokenSet(this.set.clone());
        for (var other : others) {
            other.addToTokenSet(res);
        }
        return res;
    }

    public boolean contains(TokenType token) {
        return this.set.contains(token);
    }

    public void addToken(TokenType token) {
        this.set.add(token);
    }

    public EnumSet<TokenType> getSet() {
        return this.set;
    }

    @Override
    public void addToTokenSet(TokenSet set) {
        set.set.addAll(this.set);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();

        s.append("TokenSet[");

        for (var token : this.set) {
            s.append(token);
            s.append(", ");
        }

        s.deleteCharAt(s.length() - 1);
        s.deleteCharAt(s.length() - 1);

        s.append("]");

        return s.toString();
    }
}