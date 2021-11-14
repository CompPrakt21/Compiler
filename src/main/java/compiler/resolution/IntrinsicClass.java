package compiler.resolution;

import compiler.ast.Field;

import java.util.Map;

public final class IntrinsicClass extends ClassDefinition {

    public static final IntrinsicClass STRING_CLASS = new IntrinsicClass("String");

    private final String name;

    private IntrinsicClass(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Map<String, MethodDefinition> getMethods() {
        return Map.of();
    }

    @Override
    public Map<String, Field> getFields() {
        return Map.of();
    }
}
