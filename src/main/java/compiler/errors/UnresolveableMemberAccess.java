package compiler.errors;

import compiler.ast.*;
import compiler.ast.Class;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.resolution.ClassDefinition;

public class UnresolveableMemberAccess extends CompilerError {
    private final ClassDefinition expectedClass;
    private final AstNode member;

    public UnresolveableMemberAccess(ClassDefinition expectedClass, MethodCallExpression methodCall) {
        this.expectedClass = expectedClass;
        this.member = methodCall;
    }

    public UnresolveableMemberAccess(ClassDefinition expectedClass, FieldAccessExpression fieldAccess) {
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

        this.setMessage("Can not resolve %s identifier '%s'", type, ident);

        this.addPrimaryAnnotation(ident.getSpan());

        // TODO:
        //this.addSecondaryAnnotation(this.expectedClass.getSpan(), "Expected the method in this class.");
    }
}
