package compiler.codegen.llir.nodes;

public sealed interface SideEffect permits BinaryFromMemInstruction, CallInstruction, CmpFromMemInstruction, DivInstruction, MemoryInputNode, MovLoadInstruction, MovStoreInstruction {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
