package compiler.errors;

import compiler.ast.LocalVariableDeclarationStatement;
import compiler.ast.VariableDefinition;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class IllegalLocalVariableShadowing extends CompilerError {
    private final VariableDefinition firstDefinition;
    private final LocalVariableDeclarationStatement secondDefinition;

    public IllegalLocalVariableShadowing(VariableDefinition firstDefinition, LocalVariableDeclarationStatement secondDefinition) {
        this.firstDefinition = firstDefinition;
        this.secondDefinition = secondDefinition;
    }

    @Override
    public void generate(Source source) {
        var variableName = this.secondDefinition.getIdentifier().getContent();

        this.setMessage("Redefinition of variable '%s'", variableName);

        this.addPrimaryAnnotation(this.secondDefinition.getIdentifier().getSpan());

        this.addSecondaryAnnotation(this.firstDefinition.getIdentifier().getSpan(), "first definition here.");
    }
}
