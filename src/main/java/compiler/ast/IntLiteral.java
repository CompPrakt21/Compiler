package compiler.ast;

import java.util.List;

public final class IntLiteral extends Expression {
    private String value;

    public IntLiteral(String value) {
        this.value = value;
    }

    public IntLiteral(long value) {
        this(String.valueOf(value));
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IntLiteral other)) {
            return false;
        }
        return this.value.equals(other.value);
    }
}
