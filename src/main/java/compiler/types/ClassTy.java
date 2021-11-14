package compiler.types;

import compiler.ast.Class;
import compiler.resolution.ClassDefinition;

public final class ClassTy extends Ty {
    private final ClassDefinition definition;

    public ClassTy(ClassDefinition definition) {
        this.definition = definition;
    }

    public ClassDefinition getDefinition() {
        return definition;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassTy other && other.definition == this.definition;
    }

    @Override
    public String toString() {
        return this.definition.getName();
    }
}
