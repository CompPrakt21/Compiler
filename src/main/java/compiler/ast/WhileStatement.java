package compiler.ast;

import compiler.syntax.Token;

public final class WhileStatement extends Statement {
    private final Expression condition;
    private final Statement body;

    public WhileStatement(Token whileToken, Token openParen, Expression condition, Token closeParen, Statement body) {
        super();
        this.isError |= whileToken == null || openParen == null || condition == null || closeParen == null || body == null;
        setSpan(whileToken, openParen, condition, closeParen, body);

        this.condition = condition;
        this.body = body;
    }

    public Expression getCondition() {
        return condition;
    }

    public Statement getBody() {
        return body;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof WhileStatement other)) {
            return false;
        }
        return this.condition.syntacticEq(other.condition)
                && this.body.syntacticEq(other.body);
    }
}
