package compiler.ast;

public abstract sealed class Type extends AstNode
    permits ArrayType, IntType, BoolType, VoidType, ClassType
{
}
