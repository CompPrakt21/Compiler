package compiler.errors;

import compiler.ast.Parameter;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class DuplicateParameterName extends CompilerError {

    private Parameter firstDef;
    private Parameter secondDef;

    public DuplicateParameterName(Parameter firstDef, Parameter secondDef) {
        this.firstDef = firstDef;
        this.secondDef = secondDef;
    }

    @Override
    public void generate(Source source) {
        var paramName = this.firstDef.getIdentifier().getContent();

        this.setMessage("Duplicate parameter name '%s'.", paramName);

        this.addPrimaryAnnotation(this.secondDef.getSpan(), "second definition here");
        this.addSecondaryAnnotation(this.firstDef.getSpan(), "first definition here");
    }
}
