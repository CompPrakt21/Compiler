package compiler.ast;

import java.util.List;

public final class IntLiteral extends Expression {
    private int value;

    public IntLiteral(int value) {
        this.value = value;
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return String.valueOf(value);
    }
}
