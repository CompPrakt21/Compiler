package compiler.ast;

import compiler.Token;

import java.util.List;

public final class NewObjectExpression extends Expression {
    private final ClassType typeIdentifier;

    public NewObjectExpression(Token newToken, Token typeIdentifier, Token openParen, Token closeParen) {
        super();
        this.isError |= newToken == null || typeIdentifier == null || openParen == null || closeParen == null;

        setSpan(newToken, typeIdentifier, openParen, closeParen);

        this.typeIdentifier = new ClassType(typeIdentifier);
    }

    public ClassType getType() {
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
