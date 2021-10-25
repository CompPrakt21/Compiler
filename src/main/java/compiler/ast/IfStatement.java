package compiler.ast;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(condition);
        temp.add(thenBody);
        temp.add(elseBody.get());

        return temp;
    }

    @Override
    public String getName() {
        return "IfStatement";
    }
}
