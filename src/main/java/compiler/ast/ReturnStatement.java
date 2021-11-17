package compiler.ast;

import compiler.Token;
import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ReturnStatement extends Statement {
    private final Optional<Expression> expression;

    @SuppressWarnings("ConstantConditions")
    public ReturnStatement(Token returnToken, Optional<Expression> expression, Token semicolon) {
        super();
        this.isError |= returnToken == null || expression.map(Objects::isNull).orElse(false);
        setSpan(returnToken, new OptionalWrapper(expression), semicolon);

        this.expression = expression;
    }

    public Optional<Expression> getExpression() {
        return expression;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ReturnStatement other)) {
            return false;
        }
        return OptionalUtils.combine(this.expression, other.expression, AstNode::syntacticEq).orElse(true);
    }
}
