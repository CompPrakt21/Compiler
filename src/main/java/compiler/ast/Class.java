package compiler.ast;

import java.util.List;

public final class Class extends AstNode {
    private String identifier;

    private List<Field> fields;
    private List<Method> methods;
}
