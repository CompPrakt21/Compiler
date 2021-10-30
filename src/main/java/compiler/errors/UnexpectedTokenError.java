package compiler.errors;

import compiler.Token;
import compiler.TokenSet;
import compiler.TokenSetLike;
import compiler.diagnostics.CompilerError;

import java.util.stream.Collectors;

public class UnexpectedTokenError extends CompilerError {
    public UnexpectedTokenError(Token unexpectedToken, TokenSetLike... expected) {
        super("Whooops, I didn't see this '" + unexpectedToken.type.repr + "' comming.");

        TokenSet expectedTokens = TokenSet.empty();

        for (var e : expected) {
            e.addToTokenSet(expectedTokens);
        }

        this.addPrimaryAnnotation(unexpectedToken.getSpan());
        this.addNote("Expected the following token(s): " + expectedTokens.stream().map(tty -> tty.repr).collect(Collectors.joining(", ")));
    }
}
