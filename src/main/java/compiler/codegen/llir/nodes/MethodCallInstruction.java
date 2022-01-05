package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;
import compiler.semantic.resolution.DefinedMethod;
import compiler.semantic.resolution.MethodDefinition;
import compiler.types.ArrayTy;
import compiler.types.ClassTy;

import java.util.List;
import java.util.stream.Stream;

public final class MethodCallInstruction extends CallInstruction {
    private List<RegisterNode> arguments;
    private MethodDefinition calledMethod;

    public MethodCallInstruction(BasicBlock bb, MethodDefinition calledMethod, SideEffect sideEffect, List<RegisterNode> args) {
        super(bb, sideEffect);
        this.arguments = args;
        this.calledMethod = calledMethod;

        assert !(calledMethod instanceof DefinedMethod) || args.get(0).getTargetRegister().getWidth() == Register.Width.BIT64;
        var returnTy = calledMethod.getReturnTy();
        var wideOutput = returnTy instanceof ClassTy || returnTy instanceof ArrayTy;

        initTargetRegister(wideOutput ? Register.Width.BIT64 : Register.Width.BIT32);
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

}
