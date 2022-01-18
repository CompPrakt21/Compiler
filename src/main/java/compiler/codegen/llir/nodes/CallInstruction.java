package compiler.codegen.llir.nodes;

import compiler.codegen.llir.BasicBlock;

import java.util.List;

public abstract sealed class CallInstruction extends RegisterNode  implements SideEffect permits AllocCallInstruction, MethodCallInstruction {

    protected SideEffect sideEffect;

    public CallInstruction(BasicBlock bb, SideEffect sideEffect) {
        super(bb);
        this.sideEffect = sideEffect;
    }

    public abstract List<RegisterNode> getArguments();

    public SideEffect getSideEffect() {
        return sideEffect;
    }

    @Override
    public String getMnemonic() {
        return "call";
    }
}
