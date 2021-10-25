package compiler.ast;

import java.util.List;

public final class NullExpression extends Expression {
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
