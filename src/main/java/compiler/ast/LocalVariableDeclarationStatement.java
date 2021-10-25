package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LocalVariableDeclarationStatement extends Statement {

    private Type type;
    private String identifier;

    private Optional<Expression> initializer;

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(type);
        temp.add(initializer.get());
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
    }
}
