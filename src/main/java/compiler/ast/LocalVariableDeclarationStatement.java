package compiler.ast;

import compiler.Token;
import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class LocalVariableDeclarationStatement extends Statement implements VariableDefinition {

    private final Type type;
    private final Identifier identifier;

    private final Optional<Expression> initializer;

    public LocalVariableDeclarationStatement(Type type, Token identifier, Optional<Token> assign, Optional<Expression> initializer, Token semicolon) {
        super();
        //noinspection ConstantConditions
        this.isError |= type == null || identifier == null || initializer.map(Objects::isNull).orElse(false);

        setSpan(type, identifier, new OptionalWrapper(assign), new OptionalWrapper(initializer), semicolon);

        this.type = type;
        this.identifier = new Identifier(identifier);
        this.initializer = initializer;
    }

    @Override
    public Type getType() {
        return type;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public Optional<Expression> getInitializer() {
        return initializer;
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
