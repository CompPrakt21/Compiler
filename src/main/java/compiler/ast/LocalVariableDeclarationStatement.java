package compiler.ast;

import java.util.ArrayList;
import java.util.List;
import compiler.utils.OptionalUtils;

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
    public LocalVariableDeclarationStatement(Type type, String identifier, Optional<Expression> initializer) {
        this.type = type;
        this.identifier = identifier;
        this.initializer = initializer;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof LocalVariableDeclarationStatement other)) {
            return false;
        }
        return this.type.syntacticEq(other.type)
                && this.identifier.equals(other.identifier)
                && OptionalUtils.combine(this.initializer, other.initializer, AstNode::syntacticEq).orElse(true);
    }

}
