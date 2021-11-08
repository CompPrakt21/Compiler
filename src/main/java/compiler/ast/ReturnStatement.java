package compiler.ast;

import java.util.ArrayList;
import java.util.List;

import compiler.Token;
import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

public final class ReturnStatement extends Statement {
    private Optional<Expression> expression;

    @SuppressWarnings("ConstantConditions")
    public ReturnStatement(Token returnToken, Optional<Expression> expression) {
        super();
        this.isError |= returnToken == null || expression.map(Objects::isNull).orElse(false);
        setSpan(returnToken, new OptionalWrapper(expression));

        this.expression = expression;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        expression.ifPresent(temp::add);
        return temp;
    }

    @Override
    public String getName() {
        return "ReturnStatement";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ReturnStatement other)) {
            return false;
        }
        return OptionalUtils.combine(this.expression, other.expression, AstNode::syntacticEq).orElse(true);
    }

    public Optional<Expression> getExpression() {
        return expression;
    }
}
