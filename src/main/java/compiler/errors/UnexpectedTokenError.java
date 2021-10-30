package compiler.errors;

import compiler.Token;
import compiler.TokenSet;
import compiler.TokenSetLike;
import compiler.TokenType;
import compiler.diagnostics.CompilerError;

import java.util.stream.Collectors;

public class UnexpectedTokenError extends CompilerError {
    public UnexpectedTokenError(Token unexpectedToken, TokenSetLike... expected) {
        super("");

        if (unexpectedToken.type == TokenType.Error) {
            this.setMessage(String.format("'%s' is not a valid token.", unexpectedToken.getErrorContent()));
        } else {
            this.setMessage(String.format("Unexpected token '%s'.", unexpectedToken.type.repr));
        }

        this.addPrimaryAnnotation(unexpectedToken.getSpan());

        TokenSet expectedTokens = TokenSet.empty();
        for (var e : expected) {
            e.addToTokenSet(expectedTokens);
        }
        this.addNote("Expected the following token(s): " + expectedTokens.stream().map(tty -> tty.repr).collect(Collectors.joining(", ")));
    }
}
