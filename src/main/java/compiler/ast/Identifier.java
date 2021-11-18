package compiler.ast;

import compiler.syntax.Token;
import compiler.syntax.TokenType;

public final class Identifier extends AstNode {
    private final String content;

    public Identifier(Token identifier) {
        super();
        this.isError = identifier == null;
        if (identifier != null) {
            setSpan(identifier);
            assert identifier.type == TokenType.Identifier;
            this.content = identifier.getIdentContent();
        } else {
            this.content = null;
        }
    }

    public String getContent() {
        return content;
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
