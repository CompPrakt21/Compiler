package compiler.codegen.llir.nodes;

public sealed interface SideEffect permits CallInstruction, DivInstruction, MemoryInputNode, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
