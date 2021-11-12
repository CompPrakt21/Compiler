package compiler.errors;

import compiler.Token;
import compiler.TokenSet;
import compiler.TokenSetLike;
import compiler.TokenType;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

import java.util.stream.Collectors;

public class UnexpectedTokenError extends CompilerError {
    private final Token unexpectedToken;
    private final TokenSetLike[] expected;

    public UnexpectedTokenError(Token unexpectedToken, TokenSetLike... expected) {
        this.unexpectedToken = unexpectedToken;
        this.expected = expected;
    }

    public void generate(Source source) {
        if (unexpectedToken.type == TokenType.Error) {
            var errorString = source.getSpanString(unexpectedToken.getSpan());
            this.setMessage("'%s' is not a valid token.", errorString);
            this.addNote(unexpectedToken.getErrorContent());
        } else {
            this.setMessage("Unexpected token '%s'.", unexpectedToken.type.repr);
        }

        this.addPrimaryAnnotation(unexpectedToken.getSpan());

        TokenSet expectedTokens = TokenSet.empty();
        for (var e : expected) {
            e.addToTokenSet(expectedTokens);
        }
        this.addNote("Expected the following token(s): " + expectedTokens.stream().map(tty -> tty.repr).collect(Collectors.joining(", ")));
    }
}
