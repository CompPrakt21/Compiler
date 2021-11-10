package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class AssignmentExpression extends Expression {
    private Expression lvalue;
    private Expression rvalue;

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

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(lvalue);
        temp.add(rvalue);
        return temp;
    }

    @Override
    public String getName() {
        return "AssignmentExpresssion";
    }
}
