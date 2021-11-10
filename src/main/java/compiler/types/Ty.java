package compiler.types;

import compiler.AstData;
import compiler.ast.*;
import compiler.ast.Class;

public abstract sealed class Ty permits IntTy, BoolTy, ClassTy, NoTy, ArrayTy {

    public static Ty fromAstType(Type astType, AstData<AstNode> definitions) {
        switch (astType) {
            case IntType i -> {
                return new IntTy();
            }
            case BoolType b -> {
                return new BoolTy();
            }
            case ClassType c -> {
                var klass = (Class) definitions.get(astType).get();
                return new ClassTy(klass);
            }
            case ArrayType a -> {
                var childType = fromAstType(a.getChildType(), definitions);
                return new ArrayTy(childType, 0);
            }
            case VoidType v -> throw new AssertionError("Can't convert void to type.");
        }
        throw new AssertionError("Unreacheable, the above is exhaustive");
    }
}
