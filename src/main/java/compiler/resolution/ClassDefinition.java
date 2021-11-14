package compiler.resolution;

import compiler.ast.Field;
import compiler.types.TyResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract sealed class ClassDefinition permits IntrinsicClass, DefinedClass {

    public abstract String getName();

    public abstract Map<String, MethodDefinition> getMethods();

    public abstract Map<String, Field> getFields();

    public Optional<MethodDefinition> searchMethod(String name) {
        return Optional.ofNullable(this.getMethods().get(name));
    }

    public Optional<Field> searchField(String name) {
        return Optional.ofNullable(this.getFields().get(name));
    }
}
