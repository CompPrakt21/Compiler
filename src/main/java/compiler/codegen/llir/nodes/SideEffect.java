package compiler.codegen.llir.nodes;

public sealed interface SideEffect permits CallInstruction, DivInstruction, MemoryInputNode, ModInstruction, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
