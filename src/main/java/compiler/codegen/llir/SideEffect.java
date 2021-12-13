package compiler.codegen.llir;

public sealed interface SideEffect permits DivInstruction, MemoryInputNode, ModInstruction, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
