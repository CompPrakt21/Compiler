package compiler.ast;

public final class NullExpression extends Expression {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof NullExpression;
    }
}
