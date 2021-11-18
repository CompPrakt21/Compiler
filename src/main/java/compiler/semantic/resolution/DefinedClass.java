package compiler.semantic.resolution;

import compiler.ast.Class;
import compiler.ast.Field;

import java.util.HashMap;
import java.util.Map;

public final class DefinedClass extends ClassDefinition {
    private final Class klass;

    private final Map<String, MethodDefinition> methods;
    private final Map<String, Field> fields;

    public DefinedClass(Class klass) {
        this.klass = klass;
        this.methods = new HashMap<>();
        this.fields = new HashMap<>();
    }

    void addMethod(MethodDefinition method) {
        this.methods.put(method.getName(), method);
    }

    void addField(Field field) {
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

    @Override
    public Map<String, Field> getFields() {
        return this.fields;
    }
}
