package compiler.ast;

import java.util.List;

public final class EmptyStatement extends Statement {
    @Override
    public List<AstNode> getChildren() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }
}
