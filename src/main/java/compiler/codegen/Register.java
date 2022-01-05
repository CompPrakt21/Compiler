package compiler.codegen;

public abstract sealed class Register permits VirtualRegister, HardwareRegister {

    public enum Width {
        BIT32, BIT64;
    }

    public abstract String getName();

    public abstract Width getWidth();

    @Override
    public String toString() {
        return this.getName();
    }
}
