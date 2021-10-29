package compiler.ast;

import compiler.utils.StreamUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Block extends Statement {
    private List<Statement> statements;

    public Block(List<Statement> statements) {
        this.isError |= statements.stream().anyMatch(Objects::isNull);

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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Block other)) {
            return false;
        }
        return StreamUtils.zip(this.statements.stream(), other.statements.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
