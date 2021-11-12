package compiler.ast;

import java.util.ArrayList;

import compiler.HasSpan;
import compiler.Span;
import compiler.Token;
import compiler.utils.StreamUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Method extends AstNode {

    private boolean isStatic;

    private String identifier;

    private Type returnType;

    private List<Parameter> parameters;
    private Span parametersSpan;

    private Block body;

    public Method(Token publicToken, Optional<Token> isStatic, Token identifier, Type returnType, Token openParamToken, List<Parameter> parameters, Token closeParamToken, Block body) {
        super();
        this.isError |= publicToken == null || identifier == null || returnType == null || parameters.stream().anyMatch(Objects::isNull) || body == null
                || openParamToken == null || closeParamToken == null;
        setSpan(publicToken, new HasSpan.OptionalWrapper(isStatic), identifier, returnType, new HasSpan.ListWrapper(parameters), body);

        this.isStatic = isStatic.isPresent();
        this.identifier = identifier != null ? identifier.getIdentContent() : null;
        this.returnType = returnType;
        this.parameters = parameters;

        var nonNullParams = this.parameters.stream().filter(Objects::nonNull).collect(Collectors.toList());
        if (nonNullParams.size() == 0) {
            var start = new Span(openParamToken.getSpan().start() + 1, 1);
            var end = new Span(closeParamToken.getSpan().start() - 1, 1);
            if (start.start() == end.start()) {
                this.parametersSpan = openParamToken.getSpan().merge(closeParamToken.getSpan());
            } else {
                this.parametersSpan = start.merge(end);
            }
        } else {
            this.parametersSpan = nonNullParams.stream().map(Parameter::getSpan).reduce(Span::merge).get();
        }

        this.body = body;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Parameter> getParameters() {
        return parameters;
    }

    public Span getParametersSpan() {
        return parametersSpan;
    }

    public Block getBody() {
        return body;
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
