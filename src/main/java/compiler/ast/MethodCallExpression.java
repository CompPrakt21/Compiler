package compiler.ast;

import java.util.ArrayList;
import compiler.utils.OptionalUtils;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Optional;

public final class MethodCallExpression extends Expression {
    private Optional<Expression> target;

    private String identifier;

    private List<Expression> arguments;

    public MethodCallExpression(Optional<Expression> target, String identifier, List<Expression> arguments) {
        this.target = target;
        this.identifier = identifier;
        this.arguments = arguments;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
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
