package compiler.codegen;

import java.util.List;

public abstract sealed class Operand permits MemoryLocation, Constant, Register {
    public abstract String formatIntelSyntax();

    public abstract String formatATTSyntax();

    /**
     * @return All registers that are part of the operand.
     */
    public abstract List<Register> getRegisters();
}
