package compiler.codegen;

public abstract sealed class Register extends Operand permits VirtualRegister, HardwareRegister {

    public enum Width {
        BIT32, BIT64;

        public int getByteSize() {
            return switch (this) {
                case BIT32 -> 4;
                case BIT64 -> 8;
            };
        }
    }

    public abstract String getName();

    public abstract Width getWidth();

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public String formatIntelSyntax() {
        return this.toString();
    }
}
