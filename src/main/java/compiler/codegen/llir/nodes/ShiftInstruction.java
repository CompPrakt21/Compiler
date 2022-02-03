package compiler.codegen.llir.nodes;

import compiler.codegen.HardwareRegister;
import compiler.codegen.Register;
import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public abstract sealed class ShiftInstruction extends RegisterNode permits ShiftLeftInstruction, ShiftRightInstruction, ArithmeticShiftRightInstruction {
    private RegisterNode lhs;
    private SimpleOperand rhs;

    public ShiftInstruction(BasicBlock bb, RegisterNode lhs, SimpleOperand rhs) {
        super(bb);
        this.lhs = lhs;
        this.rhs = rhs;
        assert !(rhs instanceof RegisterNode reg) || reg.getTargetRegister().getWidth().equals(Register.Width.BIT8);
        this.initTargetRegister(lhs.getTargetRegister());
    }

    public RegisterNode getLhs() {
        return lhs;
    }

    public void setLhs(RegisterNode lhs) {
        this.lhs = lhs;
    }

    public SimpleOperand getRhs() {
        return rhs;
    }

    public void setRhs(SimpleOperand rhs) {
        assert !(rhs instanceof RegisterNode reg) || reg.getTargetRegister().getWidth().equals(Register.Width.BIT8);
        this.rhs = rhs;
    }

    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.concat(Stream.of(this.lhs), this.rhs.getRegisters().stream()));
    }

    public int getPredSize() {
        return super.getPredSize() + 1 + this.rhs.getRegisters().size();
    }
}
