package compiler.codegen.llir;

import compiler.semantic.resolution.MethodDefinition;
import firm.nodes.Call;

import java.util.List;
import java.util.stream.Stream;

public final class CallInstruction extends RegisterNode  implements SideEffect{

    private SideEffect sideEffect;
    private List<RegisterNode> arguments;
    private MethodDefinition calledMethod;

    public CallInstruction(BasicBlock bb, MethodDefinition calledMethod, SideEffect sideEffect, List<RegisterNode> args) {
        super(bb);
        this.sideEffect = sideEffect;
        this.arguments = args;
        this.calledMethod = calledMethod;
        initTargetRegister();
    }

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    public List<RegisterNode> getArguments() {
        return arguments;
    }

    public MethodDefinition getCalledMethod() {
        return calledMethod;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(Stream.of(this.sideEffect.asLlirNode()), this.arguments.stream());
    }

    @Override
    public int getPredSize() {
        return this.arguments.size() + 1;
    }

    @Override
    public String getMnemonic() {
        return "call";
    }
}
