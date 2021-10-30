package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class WhileStatement extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStatement(Token whileToken, Token openParen, Expression condition, Token closeParen, Statement body) {
        this.isError |= whileToken == null || openParen == null || condition == null || closeParen == null || body == null;
        setSpan(whileToken, openParen, condition, closeParen, body);

        this.condition = condition;
        this.body = body;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(condition);
        temp.add(body);
        return temp;
    }

    @Override
    public String getName() {
        return "WhileStatement";
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
