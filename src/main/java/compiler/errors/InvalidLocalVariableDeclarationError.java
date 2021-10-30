package compiler.errors;

import compiler.ast.Statement;
import compiler.diagnostics.CompilerError;

public class InvalidLocalVariableDeclarationError extends CompilerError {
    public InvalidLocalVariableDeclarationError(Statement localvariabledecl) {
        super("Illegal variable declaration.");

        this.addPrimaryAnnotation(localvariabledecl.getSpan(), "Variable declaration can not be placed here.");

        this.addNote("""
                Local variable declaration can not follow directly
                after if, while or else.
                """);
    }
}
