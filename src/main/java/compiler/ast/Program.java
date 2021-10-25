package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class Program extends AstNode {
    private List<Class> classes;

    public Program(List<Class> classes) {
        this.classes = classes;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.addAll(classes);
        return temp;
    }

    @Override
    public String getName() {
        return "Program";
    }
}
