package compiler.ast;

import compiler.syntax.Token;

public final class AssignmentExpression extends Expression {
    private final Expression lvalue;
    private final Expression rvalue;

    public AssignmentExpression(Expression lvalue, Token assign, Expression rvalue) {
        super();
        this.isError |= lvalue == null || assign == null || rvalue == null;
        setSpan(lvalue, assign, rvalue);

        this.lvalue = lvalue;
        this.rvalue = rvalue;
    }

    public Expression getLvalue() {
        return lvalue;
    }

    public Expression getRvalue() {
        return rvalue;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof AssignmentExpression other)) {
            return false;
        }
        return this.lvalue.syntacticEq(other.lvalue)
                && this.rvalue.syntacticEq(other.rvalue);
    }
}
