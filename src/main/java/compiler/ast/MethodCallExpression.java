package compiler.ast;

import java.util.ArrayList;

import compiler.HasSpan;
import compiler.Token;
import compiler.utils.OptionalUtils;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MethodCallExpression extends Expression {
    private Optional<Expression> target;

    private String identifier;

    private List<Expression> arguments;

    public MethodCallExpression(Optional<Expression> target, Optional<Token> dot, Token identifier, Token openParen, List<Expression> arguments, Token closedParen) {
        super();
        //noinspection ConstantConditions
        this.isError |= target.map(Objects::isNull).orElse(false) || identifier == null || arguments.stream().anyMatch(Objects::isNull);

        setSpan(new HasSpan.OptionalWrapper(target), new HasSpan.OptionalWrapper(dot), identifier, openParen, new HasSpan.ListWrapper(arguments), closedParen);

        this.target = target;
        this.identifier = identifier != null ? identifier.getIdentContent() : null;
        this.arguments = arguments;
    }

    public Optional<Expression> getTarget() {
        return target;
    }

    public String getIdentifier() {
        return identifier;
    }

    public List<Expression> getArguments() {
        return arguments;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        target.ifPresent(temp::add);
        temp.addAll(arguments);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
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
