package compiler.ast;

public final class WhileStatement extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStatement(Expression condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof WhileStatement other)) {
            return false;
        }
        return this.condition.syntacticEq(other.condition)
                && this.body.syntacticEq(other.condition);
    }
}
