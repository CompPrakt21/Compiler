package compiler.ast;

import java.util.List;

public class Method extends AstNode {

    private boolean isStatic;

    private String identifier;

    private List<Parameter> parameters;

    private Block body;
}
