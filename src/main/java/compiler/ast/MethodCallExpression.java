package compiler.ast;

import java.util.ArrayList;

import compiler.HasSpan;
import compiler.Span;
import compiler.Token;
import compiler.utils.OptionalUtils;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MethodCallExpression extends Expression {
    private final Optional<Expression> target;

    private final Identifier identifier;

    private final List<Expression> arguments;

    private final Span spanWithoutTarget;

    public MethodCallExpression(Optional<Expression> target, Optional<Token> dot, Token identifier, Token openParen, List<Expression> arguments, Token closedParen) {
        super();
        //noinspection ConstantConditions
        this.isError |= target.map(Objects::isNull).orElse(false) || identifier == null || arguments.stream().anyMatch(Objects::isNull);

        setSpan(new HasSpan.OptionalWrapper(target), new HasSpan.OptionalWrapper(dot), identifier, openParen, new HasSpan.ListWrapper(arguments), closedParen);

        this.target = target;
        this.identifier = new Identifier(identifier);
        this.arguments = arguments;
        this.spanWithoutTarget = this.identifier.getSpan().merge(closedParen.getSpan());
    }

    public Optional<Expression> getTarget() {
        return target;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    public Span getSpanWithoutTarget() {
        return spanWithoutTarget;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof MethodCallExpression other)) {
            return false;
        }
        return OptionalUtils.combine(this.target, other.target, AstNode::syntacticEq).orElse(true)
                && this.identifier.equals(other.identifier)
                && StreamUtils.zip(this.arguments.stream(), other.arguments.stream(), AstNode::syntacticEq).allMatch(x -> x);
    }
}
