package compiler.ast;

import java.util.List;

public final class BoolLiteral extends Expression {
    private boolean value;

    public BoolLiteral(boolean value) {
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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof BoolLiteral other)) {
            return false;
        }
        return this.value == other.value;
    }
}
