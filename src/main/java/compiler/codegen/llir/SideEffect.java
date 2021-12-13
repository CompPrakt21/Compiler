package compiler.codegen.llir;

public sealed interface SideEffect permits CallInstruction, DivInstruction, MemoryInputNode, ModInstruction, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
