package compiler.ast;

import java.util.ArrayList;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;

public final class Class extends AstNode {
    private String identifier;

    private List<Field> fields;
    private List<Method> methods;

    public Class(String identifier, List<Field> fields, List<Method> methods) {
        this.isError |= identifier == null || fields.stream().anyMatch(Objects::isNull) || methods.stream().anyMatch(Objects::isNull);

        this.identifier = identifier;
        this.fields = fields;
        this.methods = methods;
    }


    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.addAll(fields);
        temp.addAll(methods);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
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
