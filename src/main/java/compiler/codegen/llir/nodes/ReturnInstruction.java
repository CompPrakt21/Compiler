package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ReturnInstruction extends ControlFlowNode {

    private Optional<RegisterNode> returnValue;

    public ReturnInstruction(BasicBlock bb, Optional<RegisterNode> returnValue) {
        super(bb);
        this.returnValue = returnValue;
    }

    public Optional<RegisterNode> getReturnValue() {
        return this.returnValue;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), this.returnValue.stream());
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + (this.returnValue.isPresent() ? 1 : 0);
    }

    @Override
    public String getMnemonic() {
        return "ret";
    }
}
