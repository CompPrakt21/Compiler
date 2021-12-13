package compiler.codegen.llir;

import java.util.stream.Stream;

public final class DivInstruction extends RegisterNode implements SideEffect {
    private RegisterNode dividend;
    private RegisterNode divisor;
    private SideEffect sideEffect;

    public DivInstruction(BasicBlock bb, RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect) {
        super(bb);
        this.dividend = dividend;
        this.divisor = divisor;
        this.sideEffect = sideEffect;
        initTargetRegister();
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(this.dividend, this.divisor, this.sideEffect.asLlirNode());
    }

    @Override
    public int getPredSize() {
        return 3;
    }

    @Override
    public String getMnemonic() {
        return "div";
    }
}
