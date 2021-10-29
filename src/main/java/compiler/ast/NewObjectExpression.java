package compiler.ast;

import java.util.List;

public final class NewObjectExpression extends Expression {
    private String typeIdentifier;

    public NewObjectExpression(String typeIdentifier) {
        this.isError |= typeIdentifier == null;

        this.typeIdentifier = typeIdentifier;
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
