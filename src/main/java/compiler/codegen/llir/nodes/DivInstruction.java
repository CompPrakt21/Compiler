package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

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

    public RegisterNode getDividend() {
        return dividend;
    }

    public RegisterNode getDivisor() {
        return divisor;
    }

    public SideEffect getSideEffect() {
        return sideEffect;
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
