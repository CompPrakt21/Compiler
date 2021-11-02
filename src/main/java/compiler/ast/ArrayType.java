package compiler.ast;

import compiler.Token;

import java.util.ArrayList;
import java.util.List;

public final class ArrayType extends Type {
    private final Type childType;

    public ArrayType(Type childType, Token openBracket, Token closeBracked) {
        super();
        this.isError |= childType == null || openBracket == null || closeBracked == null;
        setSpan(childType, openBracket, closeBracked);

        this.childType = childType;
    }

    public Type getChildType() {
        return childType;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(childType);
        return temp;
    }

    @Override
    public String getName() {
        return "ArrayType";
    }

    @Override
    public boolean startsNewBlock() {
        return false;
    }

    @Override
    public String getVariable() {
        return null;
    }

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof ArrayType other)) {
            return false;
        }
        return childType.syntacticEq(other.childType);
    }
}
