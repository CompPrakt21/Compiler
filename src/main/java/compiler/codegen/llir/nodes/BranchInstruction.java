package compiler.codegen.llir.nodes;

import compiler.codegen.Predicate;
import compiler.codegen.llir.BasicBlock;

import java.util.stream.Stream;

public final class BranchInstruction extends ControlFlowNode {

    private Predicate predicate;

    private CmpLikeInstruction cmp;

    public BranchInstruction(BasicBlock bb, Predicate predicate, CmpLikeInstruction cmp, BasicBlock trueBlock, BasicBlock falseBlock) {
        super(bb);
        this.predicate = predicate;
        this.cmp = cmp;
        this.targets.add(trueBlock);
        this.targets.add(falseBlock);
    }

    public BasicBlock getTrueBlock() {
        return this.targets.get(0);
    }

    public BasicBlock getFalseBlock() {
        return this.targets.get(1);
    }

    public void setTrueBlock(BasicBlock trueBlock) {
        this.targets.set(0, trueBlock);
    }

    public void setFalseBlock(BasicBlock falseBlock) {
        this.targets.set(1, falseBlock);
    }

    public Predicate getPredicate() {
        return predicate;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.concat(super.getPreds(), Stream.of((LlirNode)this.cmp));
    }

    @Override
    public int getPredSize() {
        return super.getPredSize() + 1;
    }

    @Override
    public String getMnemonic() {
        return String.format("b%s", this.predicate.getSuffix());
    }
}
