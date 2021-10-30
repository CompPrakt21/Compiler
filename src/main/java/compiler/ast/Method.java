package compiler.ast;

import java.util.ArrayList;

import compiler.HasSpan;
import compiler.Token;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Method extends AstNode {

    private boolean isStatic;

    private String identifier;

    private Type returnType;

    private List<Parameter> parameters;

    private Block body;

    public Method(Token publicToken, Optional<Token> isStatic, Token identifier, Type returnType, List<Parameter> parameters, Block body) {
        this.isError |= publicToken == null || identifier != null || returnType != null || parameters.stream().anyMatch(Objects::isNull) || body == null;
        setSpan(publicToken, new HasSpan.OptionalWrapper(isStatic), identifier, returnType, new HasSpan.ListWrapper(parameters), body);

        this.isStatic = isStatic.isPresent();
        this.identifier = identifier != null ? identifier.getIdentContent() : null;
        this.returnType = returnType;
        this.parameters = parameters;
        this.body = body;
    }

    @Override
    public List<AstNode> getChildren() {
        ArrayList<AstNode> temp = new ArrayList<>();
        temp.add(returnType);
        temp.addAll(parameters);
        temp.add(body);
        return temp;
    }

    @Override
    public String getName() {
        return identifier;
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
