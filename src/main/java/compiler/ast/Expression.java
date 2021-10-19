package compiler.ast;

public abstract sealed class Expression extends AstNode
    permits AssignmentExpression, BinaryOpExpression, UnaryExpression, MethodCallExpression, FieldAccessExpression, ArrayAccessExpression,
        BoolLiteral, IntLiteral, IdentifierExpression, ThisExpression, NewObjectExpression, NewArrayExpression
{
}
