package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

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

    public RegisterNode getNumElements() {
        return numElements;
    }

    public RegisterNode getElemSize() {
        return elemSize;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.of(this.sideEffect.asLlirNode(), this.elemSize, this.numElements));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 2;
    }

    @Override
    public List<RegisterNode> getArguments() {
        return List.of(this.elemSize, this.numElements);
    }
}
