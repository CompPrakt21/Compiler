package compiler.ast;

public abstract sealed class Statement extends AstNode
    permits Block, EmptyStatement, IfStatement, ExpressionStatement, WhileStatement, ReturnStatement, LocalVariableDeclarationStatement
{
}
