package compiler.ast;

import compiler.Token;

import java.util.List;

public final class NewObjectExpression extends Expression {
    private String typeIdentifier;

    public NewObjectExpression(Token newToken, Token typeIdentifier, Token openParen, Token closeParen) {
        this.isError |= newToken == null || typeIdentifier == null || openParen == null || closeParen == null;

        setSpan(newToken, typeIdentifier, openParen, closeParen);

        this.typeIdentifier = typeIdentifier != null ? typeIdentifier.getIdentContent() : null;
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return typeIdentifier;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof NewObjectExpression other)) {
            return false;
        }
        return this.typeIdentifier.equals(other.typeIdentifier);
    }
}
