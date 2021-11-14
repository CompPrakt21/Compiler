package compiler.resolution;

import compiler.types.IntTy;
import compiler.types.Ty;
import compiler.types.TyResult;
import compiler.types.VoidTy;

import java.util.List;
import java.util.Optional;

public final class IntrinsicMethod extends MethodDefinition {

    public static final IntrinsicMethod SYSTEM_OUT_PRINTLN = new IntrinsicMethod("System.out.println", new VoidTy(), List.of(new IntTy()));
    public static final IntrinsicMethod SYSTEM_OUT_WRITE = new IntrinsicMethod("System.out.write", new VoidTy(), List.of(new IntTy()));
    public static final IntrinsicMethod SYSTEM_OUT_FLUSH = new IntrinsicMethod("System.out.flush", new VoidTy(), List.of());
    public static final IntrinsicMethod SYSTEM_IN_READ = new IntrinsicMethod("System.in.read", new IntTy(), List.of());

    private final String name;

    private final TyResult returnTy;
    private final List<TyResult> parameterTy;

    private IntrinsicMethod(String name, TyResult returnTy, List<TyResult> parameterTy) {
        this.name = name;
        this.returnTy = returnTy;
        this.parameterTy = parameterTy;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Optional<ClassDefinition> getContainingClass() {
        return Optional.empty();
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
