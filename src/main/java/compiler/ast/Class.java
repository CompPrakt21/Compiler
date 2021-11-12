package compiler.ast;

import java.util.ArrayList;

import compiler.Token;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;

public final class Class extends AstNode {
    private Identifier identifier;

    private List<Field> fields;
    private List<Method> methods;

    public Class(Token classToken, Token identifier, Token openCurly, List<Field> fields, List<Method> methods, Token closeCurly) {
        super();
        this.isError |= classToken == null || openCurly == null || identifier == null
                || fields.stream().anyMatch(Objects::isNull) || methods.stream().anyMatch(Objects::isNull) || closeCurly == null;

        setSpan(classToken, identifier, openCurly, new ListWrapper(fields), new ListWrapper(methods), closeCurly);

        this.identifier = new Identifier(identifier);
        this.fields = fields;
        this.methods = methods;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public List<Field> getFields() {
        return fields;
    }

    public List<Method> getMethods() {
        return methods;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(identifier);
        temp.addAll(fields);
        temp.addAll(methods);
        return temp;
    }

    @Override
    public String getName() {
        return identifier.getContent();
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
