package compiler.codegen.llir.nodes;

import compiler.codegen.Register;
import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class LoadEffectiveAddressInstruction extends RegisterNode {

    private MemoryLocation loc;
    private Register.Width width;

    public LoadEffectiveAddressInstruction(BasicBlock bb, Register.Width width, MemoryLocation loc) {
        super(bb);
        this.loc = loc;
        this.width = width;
        this.initTargetRegister(width);
    }

    public MemoryLocation getLoc() {
        return loc;
    }

    @Override
    public String getMnemonic() {
        return "lea";
    }

    @Override
    public int getPredSize() {
        return loc.getRegisters().size();
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), loc.getRegisters().stream());
    }
}
