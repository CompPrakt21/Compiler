package compiler.ast;

import java.util.ArrayList;
import java.util.List;

public final class ArrayType extends Type {
    private Type childType;

    public ArrayType(Type childType) {
        this.childType = childType;
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
}
