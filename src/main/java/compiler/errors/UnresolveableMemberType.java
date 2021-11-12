package compiler.errors;

import compiler.ast.*;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class UnresolveableMemberType extends CompilerError {
    private final AstNode member;

    public UnresolveableMemberType(Method member) {
        this.member = member;
    }

    public UnresolveableMemberType(Field member) {
        this.member = member;
    }

    @Override
    public void generate(Source source) {
        String msg;
        Identifier ident;
        if (this.member instanceof Method m) {
            msg = String.format("Can not resolve return type '%s' of '%s' method.", source.getSpanString(m.getReturnType().getSpan()), m.getIdentifier());
            ident = m.getIdentifier();
        } else if (this.member instanceof Field f) {
            msg = String.format("Can not resolve type '%s' of '%s' method.", source.getSpanString(f.getType().getSpan()), f.getIdentifier());
            ident = f.getIdentifier();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        this.setMessage(msg);

        this.addPrimaryAnnotation(ident.getSpan());
    }
}
