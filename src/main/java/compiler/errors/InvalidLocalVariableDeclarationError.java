package compiler.errors;

import compiler.ast.Statement;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class InvalidLocalVariableDeclarationError extends CompilerError {
    private final Statement localVariableDecl;

    public InvalidLocalVariableDeclarationError(Statement localVariableDecl) {
        this.localVariableDecl = localVariableDecl;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Illegal variable declaration.");

        this.addPrimaryAnnotation(localVariableDecl.getSpan(), "Variable declaration can not be placed here.");

        this.addNote("""
                Local variable declaration can not follow directly
                after if, while or else.
                """);
    }
}
