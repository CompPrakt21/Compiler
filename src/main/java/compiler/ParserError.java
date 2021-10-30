package compiler;

import compiler.diagnostics.CompilerError;

import java.util.stream.Collectors;

public class ParserError extends CompilerError {
    public ParserError(Token unexpectedToken, TokenSetLike... expected) {
        super("Whooops, I didn't see this '" + unexpectedToken.type.repr + "' comming.");

        TokenSet expectedTokens = TokenSet.empty();

        for (var e : expected) {
            e.addToTokenSet(expectedTokens);
        }

        this.addPrimaryAnnotation(unexpectedToken.getSpan());
        this.addNote("Expected the following token(s): " + expectedTokens.stream().map(tty -> tty.repr).collect(Collectors.joining(", ")));
    }
}
