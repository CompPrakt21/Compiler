package compiler.types;

import compiler.ast.Class;

public final class ClassTy extends Ty {
    private Class definition;

    public ClassTy(Class definition) {
        this.definition = definition;
    }

    public Class getDefinition() {
        return definition;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ClassTy other && other.definition == this.definition;
    }

    @Override
    public String toString() {
        return this.definition.getIdentifier().getContent();
    }
}
