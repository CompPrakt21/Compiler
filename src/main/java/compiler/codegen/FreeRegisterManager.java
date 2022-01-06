package compiler.codegen;

import java.util.Optional;
import java.util.Stack;

public class FreeRegisterManager {
    private Stack<HardwareRegister> freeRegisters;

    public FreeRegisterManager() {
        this.freeRegisters = new Stack<>();

        this.freeRegisters.add(HardwareRegister.R8 );
        this.freeRegisters.add(HardwareRegister.R9 );
        this.freeRegisters.add(HardwareRegister.R10);
        this.freeRegisters.add(HardwareRegister.R11);
        this.freeRegisters.add(HardwareRegister.R12);
        this.freeRegisters.add(HardwareRegister.R13);
        this.freeRegisters.add(HardwareRegister.R14);
        this.freeRegisters.add(HardwareRegister.R15);

        this.freeRegisters.add(HardwareRegister.RSI);
        this.freeRegisters.add(HardwareRegister.RDI);
        this.freeRegisters.add(HardwareRegister.RDX);
        this.freeRegisters.add(HardwareRegister.RCX);
        this.freeRegisters.add(HardwareRegister.RBX);
        this.freeRegisters.add(HardwareRegister.RAX);
    }

    public HardwareRegister requestRegister(Register.Width width) {
        return this.freeRegisters.pop().forWidth(width);
    }

    public void releaseRegister(HardwareRegister register) {
        this.freeRegisters.push(register.forWidth(Register.Width.BIT64));
    }

    public Optional<HardwareRegister> requestSpecificRegister(HardwareRegister register) {
        var register64 = register.forWidth(Register.Width.BIT64);
        if (this.freeRegisters.contains(register64)) {
            this.freeRegisters.remove(register64);
            return Optional.of(register);
        } else {
            return Optional.empty();
        }
    }
}
