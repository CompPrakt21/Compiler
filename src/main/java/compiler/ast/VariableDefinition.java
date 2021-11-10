package compiler.ast;

public sealed interface VariableDefinition permits LocalVariableDeclarationStatement, Parameter, Field {
    Type getType();
}
