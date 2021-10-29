package compiler.ast;

import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

public final class LocalVariableDeclarationStatement extends Statement {

    private Type type;
    private String identifier;

    private Optional<Expression> initializer;

    public LocalVariableDeclarationStatement(Type type, String identifier, Optional<Expression> initializer) {
        this.isError |= type == null || identifier == null || initializer.map(Objects::isNull).orElse(false);

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
