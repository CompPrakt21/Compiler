package compiler.errors;

import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class UnresolveableMemberAccess extends CompilerError {
    private Class expectedClass;
    private AstNode member;

    public UnresolveableMemberAccess(Class expectedClass, MethodCallExpression methodCall) {
        this.expectedClass = expectedClass;
        this.member = methodCall;
    }

    public UnresolveableMemberAccess(Class expectedClass, FieldAccessExpression fieldAccess) {
        this.expectedClass = expectedClass;
        this.member = fieldAccess;
    }


    @Override
    public void generate(Source source) {
        String type;
        Identifier ident;
        if (this.member instanceof MethodCallExpression methodCall) {
            type = "method";
            ident = methodCall.getIdentifier();
        } else if (this.member instanceof FieldAccessExpression fieldAccess) {
            type = "field";
            ident = fieldAccess.getIdentifier();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        this.setMessage(String.format("Can not resolve %s identifier '%s'", type, ident));

        this.addPrimaryAnnotation(ident.getSpan());

        this.addSecondaryAnnotation(this.expectedClass.getSpan(), "Expected the method in this class.");
    }
}
