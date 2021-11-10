package compiler.errors;

import compiler.ast.AstNode;
import compiler.ast.FieldAccessExpression;
import compiler.ast.MethodCallExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;

public class MemberAccessOnNonClassType extends CompilerError {
    private AstNode member;
    private Ty targetType;

    public MemberAccessOnNonClassType(MethodCallExpression member, Ty targetType) {
        this.member = member;
        this.targetType = targetType;
    }

    public MemberAccessOnNonClassType(FieldAccessExpression member, Ty targetType) {
        this.member = member;
        this.targetType = targetType;
    }


    @Override
    public void generate(Source source) {
        String member;
        String ident;
        if (this.member instanceof MethodCallExpression methodCall) {
            member = "method";
            ident = methodCall.getIdentifier();
        } else if (this.member instanceof FieldAccessExpression fieldAccess) {
            member = "field";
            ident = fieldAccess.getIdentifier();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        this.setMessage(String.format("Can not access %s '%s' of type '%s'", member, ident, this.targetType));

        this.addPrimaryAnnotation(this.member.getSpan());
    }
}
