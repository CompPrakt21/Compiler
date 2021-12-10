package compiler.codegen.llir;

import java.util.stream.Stream;

public final class InputNode extends RegisterNode {

    public InputNode(BasicBlock bb, Register register) {
        super(bb);
        this.targetRegister = register;
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
        return "<in>";
    }
}
