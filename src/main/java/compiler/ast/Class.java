package compiler.ast;

import compiler.utils.StreamUtils;

import java.util.List;

public final class Class extends AstNode {
    private String identifier;

    private List<Field> fields;
    private List<Method> methods;

    public Class(String identifier, List<Field> fields, List<Method> methods) {
        this.identifier = identifier;
        this.fields = fields;
        this.methods = methods;
    }

    @Override
    public boolean syntacticEq(AstNode other) {
        if (!(other instanceof Class otherClass)) {
            return false;
        }
        return this.identifier.equals(otherClass.identifier)
                && StreamUtils.zip(this.fields.stream(), otherClass.fields.stream(), AstNode::syntacticEq).allMatch(x -> x)
                && StreamUtils.zip(this.methods.stream(), otherClass.methods.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
