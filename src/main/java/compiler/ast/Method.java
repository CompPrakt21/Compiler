package compiler.ast;

import java.util.List;

public class Method extends AstNode {

    private boolean isStatic;

    private String identifier;

    private Type returnType;

    private List<Parameter> parameters;

    private Block body;

    public Method(boolean isStatic, String identifier, Type returnType, List<Parameter> parameters, Block body) {
        this.isStatic = isStatic;
        this.identifier = identifier;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
    }
}
