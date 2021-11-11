package compiler.errors;

import compiler.ast.AstNode;
import compiler.ast.FieldAccessExpression;
import compiler.ast.MethodCallExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;
import compiler.types.UnresolveableTy;
import compiler.types.VoidTy;

public class MemberAccessOnNonClassType extends CompilerError {
    private AstNode member;
    private TyResult targetType;

    public MemberAccessOnNonClassType(MethodCallExpression member, TyResult targetType) {
        this.member = member;
        this.targetType = targetType;
    }

    public MemberAccessOnNonClassType(FieldAccessExpression member, TyResult targetType) {
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

        var typeString = switch (this.targetType) {
            case Ty ty -> String.format("of type '%s'", ty);
            case UnresolveableTy ty -> "";
            case VoidTy ty -> "of type 'void'";
        };

        this.setMessage(String.format("Can not access %s '%s' ", member, ident, typeString));

        this.addPrimaryAnnotation(this.member.getSpan());
    }
}
