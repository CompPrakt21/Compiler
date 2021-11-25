package compiler.types;

import compiler.ast.Class;
import compiler.ast.Field;
import compiler.semantic.resolution.MethodDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class DefinedClassTy extends ClassTy {
    private final Class klass;
    private final Map<String, MethodDefinition> methods;
    private final Map<String, Field> fields;

    public DefinedClassTy(Class klass) {
        this.klass = klass;
        this.methods = new HashMap<>();
        this.fields = new HashMap<>();
    }

    public void addMethod(MethodDefinition method) {
        this.methods.put(method.getName(), method);
    }

    public void addField(Field field) {
        this.fields.put(field.getIdentifier().getContent(), field);
    }

    public Class getAstClass() {
        return klass;
    }

    @Override
    public String getName() {
        return this.klass.getIdentifier().getContent();
    }

    @Override
    public Map<String, MethodDefinition> getMethods() {
        return this.methods;
    }

    public Map<String, Field> getFields() {
        return this.fields;
    }

    public Optional<Field> searchField(String name) {
        return Optional.ofNullable(this.getFields().get(name));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefinedClassTy that = (DefinedClassTy) o;
        return this.klass == that.klass;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this.klass);
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
