package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class MemoryInputNode extends LlirNode implements SideEffect {

    public MemoryInputNode(BasicBlock bb) {
        super(bb);
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return super.getPreds();
    }

    @Override
    public int getPredSize() {
        return super.getPredSize();
    }

    @Override
    public String getMnemonic() {
        return "<mem-input>";
    }
}
