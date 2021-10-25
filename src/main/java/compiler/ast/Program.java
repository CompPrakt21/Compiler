package compiler.ast;

import java.util.List;

public final class Program extends AstNode {
    private List<Class> classes;

    public Program(List<Class> classes) {
        this.classes = classes;
    }
}
