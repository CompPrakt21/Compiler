package compiler.ast;

import java.util.List;

public final class NewObjectExpression extends Expression {
    private String typeIdentifier;
    public NewObjectExpression(String typeIdentifier) {
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
}
