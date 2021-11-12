package compiler.errors;

import compiler.ast.ClassType;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class ClassDoesNotExist extends CompilerError {

    private ClassType type;

    public ClassDoesNotExist(ClassType type) {
        this.type = type;
    }

    @Override
    public void generate(Source source) {
        this.setMessage(String.format("Class with name '%s' does not exist.", this.type.getIdentifier()));
        this.addPrimaryAnnotation(this.type.getSpan());
    }
}
