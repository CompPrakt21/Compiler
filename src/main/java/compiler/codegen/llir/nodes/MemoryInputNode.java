package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class MemoryInputNode extends LlirNode implements SideEffect {

    public MemoryInputNode(BasicBlock bb) {
        super(bb);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.empty();
    }

    @Override
    public int getPredSize() {
        return 0;
    }

    @Override
    public String getMnemonic() {
        return "<mem-input>";
    }
}
