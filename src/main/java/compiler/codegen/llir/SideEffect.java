package compiler.codegen.llir;

public sealed interface SideEffect permits MemoryInputNode {
    default LlirNode asLlirNode() {
        return (LlirNode) this;
    }
}
