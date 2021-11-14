package compiler.errors;

import compiler.ast.AstNode;
import compiler.ast.Field;
import compiler.ast.Identifier;
import compiler.ast.Method;
import compiler.diagnostics.CompilerError;
import compiler.diagnostics.Source;

public class MultipleUseOfSameMemberName extends CompilerError {
    private AstNode firstUse;
    private AstNode secondUse;

    public MultipleUseOfSameMemberName(Method firstUse, Method secondUse) {
        this.firstUse = firstUse;
        this.secondUse = secondUse;
    }

    public MultipleUseOfSameMemberName(Field firstUse, Field secondUse) {
        this.firstUse = firstUse;
        this.secondUse = secondUse;
    }

    @Override
    public void generate(Source source) {
        String member;
        Identifier firstIdent;
        if (this.firstUse instanceof Method method) {
            member = "method";
            firstIdent = method.getIdentifier();
        } else if (this.firstUse instanceof Field field) {
            member = "field";
            firstIdent = field.getIdentifier();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        Identifier secondIdent;
        if (this.secondUse instanceof Method method) {
            secondIdent = method.getIdentifier();
        } else if (this.secondUse instanceof Field field) {
            secondIdent = field.getIdentifier();
        } else {
            throw new AssertionError("Unreacheable because of constructor");
        }

        this.setMessage("Reuse of %s name '%s'", member, firstIdent.getContent());

        this.addPrimaryAnnotation(secondIdent.getSpan());

        this.addSecondaryAnnotation(firstIdent.getSpan(), "defined here first");
    }
}
