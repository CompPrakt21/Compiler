package compiler.ast;

import compiler.HasSpan;
import compiler.Token;
import compiler.utils.StreamUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Block extends Statement {
    private List<Statement> statements;

    public Block(Token openCurly, List<Statement> statements, Token closedCurly) {
        this.isError |= statements.stream().anyMatch(Objects::isNull);
        setSpan(openCurly, new HasSpan.ListWrapper(statements), closedCurly);

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

    public List<Statement> getStatements() {
        return statements;
    }
}
