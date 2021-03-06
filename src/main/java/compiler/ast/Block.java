package compiler.ast;

import compiler.syntax.HasSpan;
import compiler.syntax.Token;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;

public final class Block extends Statement {
    private final List<Statement> statements;

    public Block(Token openCurly, List<Statement> statements, Token closedCurly) {
        super();
        this.isError |= statements.stream().anyMatch(Objects::isNull);
        setSpan(openCurly, new HasSpan.ListWrapper(statements), closedCurly);

        this.statements = statements;
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Block other)) {
            return false;
        }
        return StreamUtils.zip(this.statements.stream(), other.statements.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
