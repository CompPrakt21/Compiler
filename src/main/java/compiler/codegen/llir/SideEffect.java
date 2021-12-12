package compiler.codegen.llir;

public sealed interface SideEffect permits MemoryInputNode, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
