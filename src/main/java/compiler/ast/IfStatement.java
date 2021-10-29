package compiler.ast;

import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

public final class IfStatement extends Statement {
    private Expression condition;
    private Statement thenBody;
    private Optional<Statement> elseBody;

    public IfStatement(Expression condition, Statement thenBody, Optional<Statement> elseBody) {
        this.isError |= condition == null || thenBody == null || elseBody.map(Objects::isNull).orElse(false);

        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IfStatement other)) {
            return false;
        }
        return this.condition.syntacticEq(other.condition)
                && this.thenBody.syntacticEq(other.thenBody)
                && OptionalUtils.combine(this.elseBody, other.elseBody, AstNode::syntacticEq).orElse(true);
    }
}
