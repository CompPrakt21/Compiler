package compiler.errors;

import compiler.ast.AstNode;
import compiler.ast.Field;
import compiler.ast.Method;
import compiler.ast.Type;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class UnresolveableMemberType extends CompilerError {
    private AstNode member;

    public UnresolveableMemberType(Method member) {
        this.member = member;
    }

    public UnresolveableMemberType(Field member) {
        this.member = member;
    }

    @Override
    public void generate(Source source) {
        String msg;
        Type type;
        if (this.member instanceof Method m) {
            msg = String.format("Can not resolve return type '%s' of '%s' method.", source.getSpanString(m.getReturnType().getSpan()), m.getIdentifier());
            type = m.getReturnType();
        } else if (this.member instanceof Field f) {
            msg = String.format("Can not resolve type '%s' of '%s' method.", source.getSpanString(f.getType().getSpan()), f.getIdentifier());
            type = f.getType();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        this.setMessage(msg);

        this.addPrimaryAnnotation(this.member.getSpan());
    }
}
