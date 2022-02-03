package compiler.codegen.sir.instructions;

import compiler.codegen.HardwareRegister;
import compiler.codegen.MemoryLocation;
import compiler.codegen.Operand;
import compiler.codegen.Register;

import java.util.List;
import java.util.stream.Stream;

public abstract sealed class ShiftInstruction extends RegisterInstruction permits ShiftLeftInstruction, ShiftRightInstruction, ArithmeticShiftRightInstruction {
    private Register lhs;
    private Operand rhs;

    public ShiftInstruction(Register target, Register lhs, Operand rhs) {
        super(target);
        this.lhs = lhs;
        this.rhs = rhs;
        this.verifyRhs();
    }

    private void verifyRhs() {
        assert !(rhs instanceof MemoryLocation);
        if (this.rhs instanceof Register reg) {
            assert reg.getWidth().equals(Register.Width.BIT8);
            assert !(reg instanceof HardwareRegister hardwareReg) || hardwareReg.equals(HardwareRegister.CL);
        }
    }

    public Register getLhs() {
        return lhs;
    }

    public void setLhs(Register lhs) {
        this.lhs = lhs;
    }

    public Operand getRhs() {
        return rhs;
    }

    public void setRhs(Operand rhs) {
        this.verifyRhs();
        this.rhs = rhs;
    }

    @Override
    public List<Register> getReadRegisters() {
        return Stream.concat(Stream.of(this.lhs), this.rhs.getRegisters().stream()).toList();
    }
}
