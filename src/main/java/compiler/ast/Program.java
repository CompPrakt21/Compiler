package compiler.ast;

import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;

public final class Program extends AstNode {
    private List<Class> classes;

    public Program(List<Class> classes) {
        this.isError |= classes.stream().anyMatch(Objects::isNull);

        this.classes = classes;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Program other)) {
            return false;
        }
        return StreamUtils.zip(this.classes.stream(), other.classes.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
