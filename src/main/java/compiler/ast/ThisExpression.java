package compiler.ast;

import compiler.Token;

import java.util.List;

public final class ThisExpression extends Expression {
    public ThisExpression(Token thisToken) {
        setSpan(thisToken);
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
        return otherAst instanceof ThisExpression;
    }
}
