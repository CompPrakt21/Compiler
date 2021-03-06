package compiler.errors;

import compiler.ast.AstNode;
import compiler.ast.FieldAccessExpression;
import compiler.ast.Identifier;
import compiler.ast.MethodCallExpression;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;
import compiler.types.UnresolveableTy;
import compiler.types.VoidTy;

public class MemberAccessOnNonClassType extends CompilerError {
    private final AstNode member;
    private final TyResult targetType;

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
        Identifier ident;
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
            case UnresolveableTy ignored -> "";
            case VoidTy ignored -> "of type 'void'";
        };

        this.setMessage("Can not access %s '%s' ", member, ident, typeString);

        this.addPrimaryAnnotation(ident.getSpan());
    }
}
