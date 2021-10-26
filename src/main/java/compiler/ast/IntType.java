package compiler.ast;

public final class IntType extends Type {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof IntType;
    }
}
