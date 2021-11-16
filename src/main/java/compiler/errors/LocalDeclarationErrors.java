package compiler.errors;

import compiler.ast.LocalVariableDeclarationStatement;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;
import compiler.types.Ty;
import compiler.types.TyResult;

public class LocalDeclarationErrors {
    public static class UnresolveableType extends CompilerError {
        private final LocalVariableDeclarationStatement stmt;

        public UnresolveableType(LocalVariableDeclarationStatement stmt) {
            this.stmt = stmt;
        }

        @Override
        public void generate(Source source) {
            var typeStr = source.getSpanString(this.stmt.getType().getSpan());
            this.setMessage("Can not resolve type '%s' of local variable declaration.", typeStr);

            this.addPrimaryAnnotation(this.stmt.getType().getSpan());
        }
    }

    public static class VoidType extends CompilerError {
        private final LocalVariableDeclarationStatement stmt;

        public VoidType(LocalVariableDeclarationStatement stmt) {
            this.stmt = stmt;
        }

        @Override
        public void generate(Source source) {
            this.setMessage("Can not create local variables with void type");

            this.addPrimaryAnnotation(this.stmt.getType().getSpan());
        }
    }

    public static class TypeMismatch extends CompilerError {
        private final LocalVariableDeclarationStatement stmt;
        private final Ty expectedTy;

        private final TyResult actualTy;

        public TypeMismatch(LocalVariableDeclarationStatement stmt, Ty expectedTy, TyResult actualTy) {
            this.stmt = stmt;
            this.expectedTy = expectedTy;
            this.actualTy = actualTy;
        }

        @Override
        public void generate(Source source) {
            assert this.stmt.getInitializer().isPresent();
            this.setMessage("Can not assign value of type '%s' to local variable '%s' with type '%s'", this.actualTy, this.stmt.getIdentifier(), this.expectedTy);
            this.addPrimaryAnnotation(this.stmt.getInitializer().get().getSpan(), "this has type '%s'", this.actualTy);
            this.addSecondaryAnnotation(this.stmt.getType().getSpan(), "this is type '%s'", this.expectedTy);
        }
    }

    public static class StringUsed extends CompilerError {
        private final LocalVariableDeclarationStatement stmt;

        public StringUsed(LocalVariableDeclarationStatement stmt) {this.stmt = stmt;}

        @Override
        public void generate(Source source) {
            this.setMessage("Cannot create new String class");
            this.addPrimaryAnnotation(stmt.getType().getSpan());
        }
    }
}
