package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class WhileStatement extends Statement {
    private Expression condition;
    private Statement body;

    public WhileStatement(Expression condition, Statement body) {
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
}
