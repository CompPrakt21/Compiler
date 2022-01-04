package compiler.codegen.llir;

public abstract sealed class Register permits VirtualRegister, HardwareRegister {

    public enum Width {
        BIT32, BIT64;
    }

    public abstract String getName();

    public abstract Width getWidth();
}
