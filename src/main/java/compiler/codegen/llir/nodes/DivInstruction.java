package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.Register;

import java.util.stream.Stream;

public final class DivInstruction extends RegisterNode implements SideEffect {
    private RegisterNode dividend;
    private RegisterNode divisor;
    private SideEffect sideEffect;
    private DivType type;

    public enum DivType {
        Div, Mod
    }

    public DivInstruction(BasicBlock bb, RegisterNode dividend, RegisterNode divisor, SideEffect sideEffect, DivType type) {
        super(bb);
        this.dividend = dividend;
        this.divisor = divisor;
        this.sideEffect = sideEffect;
        this.type = type;
        initTargetRegister(Register.Width.BIT32);
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

    public DivType getType() {
        return type;
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
        return switch (this.type) {
            case Div -> "div";
            case Mod -> "div (mod)";
        };
    }
}
