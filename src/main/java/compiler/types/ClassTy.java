package compiler.types;

import compiler.semantic.resolution.MethodDefinition;

import java.util.Map;
import java.util.Optional;

public abstract sealed class ClassTy extends Ty permits DefinedClassTy, IntrinsicClassTy {

    public abstract String getName();

    public abstract Map<String, MethodDefinition> getMethods();

    public Optional<MethodDefinition> searchMethod(String name) {
        return Optional.ofNullable(this.getMethods().get(name));
    }
}
