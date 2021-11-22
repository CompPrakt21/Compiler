package compiler.types;

import compiler.semantic.resolution.MethodDefinition;

import java.util.Map;

public final class IntrinsicClassTy extends ClassTy {
    private final String name;

    public static final IntrinsicClassTy STRING_CLASS = new IntrinsicClassTy("String");

    public IntrinsicClassTy(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public Map<String, MethodDefinition> getMethods() {
        return Map.of();
    }
}
