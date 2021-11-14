package compiler.resolution;

import compiler.ast.Method;
import compiler.types.TyResult;

import java.util.List;
import java.util.Optional;

public sealed abstract class MethodDefinition permits DefinedMethod, IntrinsicMethod {
    public abstract String getName();

    public abstract Optional<ClassDefinition> getContainingClass();

    public abstract TyResult getReturnTy();

    public abstract List<TyResult> getParameterTy();
}
