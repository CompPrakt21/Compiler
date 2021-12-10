package compiler.codegen.llir;

public abstract sealed class Register permits VirtualRegister, HardwareRegister {
    public abstract String getName();
}
