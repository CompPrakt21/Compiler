package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class Block extends Statement {
    private List<Statement> statements;

    public Block(List<Statement> statements) {
        this.statements = statements;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.addAll(statements);
        return temp;
    }

    @Override
    public String getName() {
        return "Block";
    }
}
