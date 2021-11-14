package compiler.resolution;

import compiler.AstData;
import compiler.ast.*;
import compiler.ast.Class;

import java.util.Optional;

public class Definitions {
    private final AstData<Object> inner;

    public Definitions() {
        this.inner = new AstData<>();
    }

    public AstData<Object> getInner() {
        return this.inner;
    }

    public void setMethod(MethodCallExpression methodCall, MethodDefinition method) {
        this.inner.set(methodCall, method);
    }

    public Optional<MethodDefinition> getMethod(MethodCallExpression methodCall) {
        var method = this.inner.get(methodCall);
        assert method.map(o -> o instanceof MethodDefinition).orElse(true);
        return method.map(o -> (MethodDefinition) o);
    }

    public void setField(FieldAccessExpression fieldAccess, Field field) {
        this.inner.set(fieldAccess, field);
    }

    public Optional<Field> getField(FieldAccessExpression fieldAccess) {
        var field = this.inner.get(fieldAccess);
        assert field.map(o -> o instanceof Field).orElse(true);
        return field.map(o -> (Field) o);
    }

    public void setReference(Reference ref, VariableDefinition def) {
        this.inner.set(ref, def);
    }

    public Optional<VariableDefinition> getReference(Reference ref) {
        var ast = this.inner.get(ref);
        assert ast.map(o -> o instanceof VariableDefinition).orElse(true);
        return ast.map(o -> (VariableDefinition) o);
    }

    public void setClass(ClassType classType, ClassDefinition classDef) {
        this.inner.set(classType, classDef);
    }

    public Optional<ClassDefinition> getClass(ClassType classType) {
        var klass = this.inner.get(classType);
        assert klass.map(o -> o instanceof ClassDefinition).orElse(true);
        return klass.map(o -> (ClassDefinition) o);
    }
}
