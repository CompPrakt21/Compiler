package compiler.codegen.llir;

public non-sealed abstract class EffectNode extends LlirNode {
    public EffectNode(BasicBlock basicBlock) {
        super(basicBlock);
    }
}
