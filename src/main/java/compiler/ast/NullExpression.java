package compiler.ast;

import compiler.Token;

import java.util.List;

public final class NullExpression extends Expression {
    public NullExpression(Token nullToken) {
        setSpan(nullToken);
    }

    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof NullExpression;
    }
}
