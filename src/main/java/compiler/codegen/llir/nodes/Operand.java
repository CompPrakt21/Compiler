package compiler.codegen.llir.nodes;

import java.util.List;

public sealed interface Operand permits SimpleOperand, MemoryLocation {
    public String formatIntelSyntax();

    public List<RegisterNode> getRegisters();
}
