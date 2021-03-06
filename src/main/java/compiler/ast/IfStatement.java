package compiler.ast;

import compiler.syntax.Token;
import compiler.utils.OptionalUtils;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class IfStatement extends Statement {
    private final Expression condition;
    private final Statement thenBody;
    private final Optional<Statement> elseBody;

    public IfStatement(Token ifToken, Token openParen, Expression condition, Token closeParen, Statement thenBody, Optional<Statement> elseBody) {
        super();
        this.isError |= ifToken == null || openParen == null || condition == null || closeParen == null || thenBody == null;

        setSpan(ifToken, openParen, condition, closeParen, thenBody, new OptionalWrapper(elseBody));

        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getThenBody() {
        return thenBody;
    }

    public Optional<Statement> getElseBody() {
        return elseBody;
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
