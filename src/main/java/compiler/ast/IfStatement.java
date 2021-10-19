package compiler.ast;

import java.util.Optional;

public final class IfStatement extends Statement {
    private Expression condition;
    private Statement thenBody;
    private Optional<Statement> elseBody;
}
