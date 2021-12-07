package compiler.codegen;

import java.util.stream.Stream;

public final class InputNode extends RegisterNode {

    public InputNode(Register register) {
        super(register);
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
