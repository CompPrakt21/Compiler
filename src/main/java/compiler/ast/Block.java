package compiler.ast;

import java.util.List;

public final class Block extends Statement {
    private List<Statement> statements;

    public Block(List<Statement> statements) {
        this.statements = statements;
    }
}
