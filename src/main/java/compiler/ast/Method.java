package compiler.ast;

import compiler.utils.StreamUtils;

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

    @Override
    public boolean syntacticEq(AstNode otherAst) {
        if (!(otherAst instanceof Method other)) {
            return false;
        }
        return this.isStatic == other.isStatic
                && this.identifier.equals(other.identifier)
                && this.returnType.syntacticEq(other.returnType)
                && StreamUtils.zip(this.parameters.stream(), other.parameters.stream(), AstNode::syntacticEq).allMatch(x -> x)
                && this.body.syntacticEq(other.body);
    }
}
