package compiler.codegen.llir;

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
        return this.returnValue.stream().map(regNode -> regNode);
    }

    @Override
    public int getPredSize() {
        return this.returnValue.isPresent() ? 1 : 0;
    }

    @Override
    public String getMnemonic() {
        return "ret";
    }
}
