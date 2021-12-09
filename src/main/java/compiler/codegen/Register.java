package compiler.codegen;

public abstract sealed class Register permits VirtualRegister, HardwareRegister {
    public abstract String getName();
}
