package compiler.ast;

public final class WhileStatement extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStatement(Expression condition, Statement body) {
        this.condition = condition;
        this.body = body;
    }
}
