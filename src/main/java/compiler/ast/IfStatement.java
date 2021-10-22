package compiler.ast;

import java.util.Optional;

public final class IfStatement extends Statement {
    private Expression condition;
    private Statement thenBody;
    private Optional<Statement> elseBody;

    public IfStatement(Expression condition, Statement thenBody, Optional<Statement> elseBody) {
        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }
}
