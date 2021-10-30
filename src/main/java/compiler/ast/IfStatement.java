package compiler.ast;

import java.util.ArrayList;
import java.util.List;

import compiler.Token;
import compiler.utils.OptionalUtils;

import java.util.Objects;
import java.util.Optional;

public final class IfStatement extends Statement {
    private Expression condition;
    private Statement thenBody;
    private Optional<Statement> elseBody;

    public IfStatement(Token ifToken, Token openParen, Expression condition, Token closeParen, Statement thenBody, Optional<Statement> elseBody) {
        this.isError |= ifToken == null || openParen == null || condition == null || closeParen == null || thenBody == null
                || elseBody.map(Objects::isNull).orElse(false);

        setSpan(ifToken, openParen, condition, closeParen, thenBody, new OptionalWrapper(elseBody));

        this.condition = condition;
        this.thenBody = thenBody;
        this.elseBody = elseBody;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(condition);
        temp.add(thenBody);
        elseBody.ifPresent(temp::add);

        return temp;
    }

    @Override
    public String getName() {
        return "IfStatement";
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof IfStatement other)) {
            return false;
        }
        return this.condition.syntacticEq(other.condition)
                && this.thenBody.syntacticEq(other.thenBody)
                && OptionalUtils.combine(this.elseBody, other.elseBody, AstNode::syntacticEq).orElse(true);
    }
}
