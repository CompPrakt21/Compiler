package compiler.semantic.resolution;

import compiler.types.ClassTy;
import compiler.types.TyResult;

import java.util.List;
import java.util.Optional;

public sealed abstract class MethodDefinition permits DefinedMethod, IntrinsicMethod {
    public abstract String getName();

    public abstract Optional<ClassTy> getContainingClass();

    public abstract TyResult getReturnTy();

    public abstract List<TyResult> getParameterTy();

    public abstract String getLinkerName();
}
