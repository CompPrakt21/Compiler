package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.Register;

import java.util.List;
import java.util.stream.Stream;

public final class AllocCallInstruction extends CallInstruction {

    private RegisterNode numElements;
    private RegisterNode elemSize;

    public AllocCallInstruction(BasicBlock bb, SideEffect sideEffect, RegisterNode numElements, RegisterNode elemSize) {
        super(bb, sideEffect);
        this.numElements = numElements;
        this.elemSize = elemSize;
        initTargetRegister(Register.Width.BIT64);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(this.sideEffect.asLlirNode(), this.elemSize, this.numElements);
    }

    @Override
    public int getPredSize() {
        return 2;
    }

    @Override
    public List<RegisterNode> getArguments() {
        return List.of(this.elemSize, this.numElements);
    }
}
