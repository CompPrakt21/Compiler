package compiler.ast;

import compiler.Token;

import java.util.List;

public final class IntLiteral extends Expression {
    private String value;

    public IntLiteral(Token value) {
        this.isError |= value == null;
        setSpan(value);

        this.value = value != null ? value.getIntLiteralContent() : null;
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return value;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IntLiteral other)) {
            return false;
        }
        return this.value.equals(other.value);
    }

    public String getValue() {
        return value;
    }
}
