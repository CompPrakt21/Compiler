package compiler.ast;

import compiler.HasSpan;
import compiler.Token;
import compiler.TokenType;

import java.util.List;

public final class Identifier extends AstNode {
    private String content;

    public Identifier(Token identifier) {
        super();
        this.isError = identifier == null;
        if (identifier != null) {
            setSpan(identifier);
            assert identifier.type == TokenType.Identifier;
            this.content = identifier.getIdentContent();
        }
    }

    public String getContent() {
        return content;
    }

    @Override
    public List<AstNode> getChildren() {
        return List.of();
    }

    @Override
    public String getName() {
        return "Identifier";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof Identifier otherIdent && otherIdent.content.equals(this.content);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AstNode other && this.syntacticEq(other);
    }

    @Override
    public String toString() {
        return this.content;
    }
}
