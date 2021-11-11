package compiler.types;

import compiler.AstData;
import compiler.ast.*;
import compiler.ast.Class;

public abstract sealed class Ty extends TyResult permits IntTy, BoolTy, ClassTy, NoTy, ArrayTy {

}
