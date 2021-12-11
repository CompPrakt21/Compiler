package compiler.codegen.llir;

import firm.nodes.Cmp;

import java.util.stream.Stream;

public final class BranchInstruction extends ControlFlowNode {

    public enum Predicate {
        GREATER_THAN("gt"), GREATER_EQUAL("ge"), LESS_THAN("lt"), LESS_EQUAL("le"), EQUAL("eq");

        private String suffix;

        Predicate(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    private Predicate predicate;

    private CmpInstruction cmp;

    public BranchInstruction(BasicBlock bb, Predicate predicate, CmpInstruction cmp, BasicBlock trueBlock, BasicBlock falseBlock) {
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

    public Predicate getPredicate() {
        return predicate;
    }

    @Override
    public Stream<LlirNode> getPreds() {
        return Stream.of(this.cmp);
    }

    @Override
    public int getPredSize() {
        return 1;
    }

    @Override
    public String getMnemonic() {
        return String.format("b%s", this.predicate.getSuffix());
    }
}
