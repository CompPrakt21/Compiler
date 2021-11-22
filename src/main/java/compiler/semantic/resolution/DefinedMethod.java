package compiler.semantic.resolution;

import compiler.ast.Method;
import compiler.types.ClassTy;
import compiler.types.TyResult;

import java.util.List;
import java.util.Optional;

public final class DefinedMethod extends MethodDefinition {
    private Method method;
    private ClassTy containingClass;

    private TyResult returnTy;

    private List<TyResult> parameterTy;

    public DefinedMethod(Method method, ClassTy containingClass, TyResult returnTy, List<TyResult> parameterTy) {
        this.method = method;
        this.containingClass = containingClass;
        this.returnTy = returnTy;
        this.parameterTy = parameterTy;
    }

    public Method getAstMethod() {
        return this.method;
    }

    @Override
    public String getName() {
        return method.getIdentifier().getContent();
    }

    @Override
    public Optional<ClassTy> getContainingClass() {
        return Optional.of(this.containingClass);
    }

    @Override
    public TyResult getReturnTy() {
        return this.returnTy;
    }

    @Override
    public List<TyResult> getParameterTy() {
        return this.parameterTy;
    }
}
