package compiler.ast;

public final class ThisExpression extends Expression {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof ThisExpression;
    }
}
