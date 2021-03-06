package compiler.errors;

import compiler.ast.Reference;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class UnresolveableReference extends CompilerError {
    private final Reference ref;

    public UnresolveableReference(Reference ref) {
        this.ref = ref;
    }

    @Override
    public void generate(Source source) {
        this.setMessage("Can not resolve identifier '%s'.", this.ref.getIdentifier());

        this.addPrimaryAnnotation(ref.getSpan());
    }
}
