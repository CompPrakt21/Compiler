package compiler.errors;

import compiler.ast.Class;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.resolution.ClassDefinition;
import compiler.resolution.DefinedClass;

public class MultipleUseOfSameClassName extends CompilerError {
    private final ClassDefinition firstUse;
    private final Class secondUse;

    public MultipleUseOfSameClassName(ClassDefinition firstUse, Class secondUse) {
        this.firstUse = firstUse;
        this.secondUse = secondUse;
    }

    @Override
    public void generate(Source source) {

        var name = this.firstUse.getName();

        this.setMessage("Reuse of class name '%s'", name);

        this.addPrimaryAnnotation(secondUse.getIdentifier().getSpan());

        if (this.firstUse instanceof DefinedClass dc) {
            this.addSecondaryAnnotation(dc.getAstClass().getIdentifier().getSpan(), "first definition here");
        } else {
            this.addNote("%s is an intrinsic class.", name);
        }
    }
}
