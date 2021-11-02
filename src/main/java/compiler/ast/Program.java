package compiler.ast;

import java.util.ArrayList;

import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;

public final class Program extends AstNode {

    private final List<Class> classes;

    public Program(List<Class> classes) {
        super();
        this.isError |= classes.stream().anyMatch(Objects::isNull);

        setSpan(new ListWrapper(classes));

        this.classes = classes;
    }

    public List<Class> getClasses() {
        return classes;
    }

    @Override
    public List<AstNode> getChildren() {
        return new ArrayList<>(classes);
    }

    @Override
    public String getName() {
        return "Program";
    }

    @Override
    public boolean startsNewBlock() {
        return true;
    }

    @Override
    public String getVariable() {
        return null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Program other)) {
            return false;
        }
        return StreamUtils.zip(this.classes.stream(), other.classes.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
