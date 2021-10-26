package compiler.ast;

public final class BoolType extends Type {

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        return otherAst instanceof BoolType;
    }
}
