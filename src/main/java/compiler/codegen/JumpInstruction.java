package compiler.codegen;

import java.util.List;

public class JumpInstruction extends ControlFlowNode {

    public JumpInstruction(BasicBlock target, List<LlirNode> preds) {
        super(preds);
        this.targets.add(0, target);
    }

    public BasicBlock getTarget() {
        return this.targets.get(0);
    }

    @Override
    public String getMnemonic() {
        return "jmp";
    }
}
