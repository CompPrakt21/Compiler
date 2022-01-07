package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.Register;

import java.util.stream.Stream;

public final class InputNode extends RegisterNode {

    public InputNode(BasicBlock bb, Register register) {
        super(bb);
        this.targetRegister = register;
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
        return "<in>";
    }
}
