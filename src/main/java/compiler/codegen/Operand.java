package compiler.codegen;

public abstract sealed class Operand permits MemoryLocation, Constant, Register {
    public abstract String formatIntelSyntax();

    public abstract String formatATTSyntax();
}
