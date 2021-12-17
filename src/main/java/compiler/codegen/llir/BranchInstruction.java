package compiler.codegen.llir;

import firm.nodes.Cmp;

import java.util.stream.Stream;

public final class BranchInstruction extends ControlFlowNode {

    public enum Predicate {
        GREATER_THAN("g"), GREATER_EQUAL("ge"), LESS_THAN("l"), LESS_EQUAL("le"), EQUAL("e"), NOT_EQUAL("ne");

        private String suffix;

        Predicate(String suffix) {
            this.suffix = suffix;
        }

        public Predicate invert() {
            return switch (this) {
                case GREATER_THAN -> LESS_EQUAL;
                case GREATER_EQUAL -> LESS_THAN;
                case LESS_THAN -> GREATER_EQUAL;
                case LESS_EQUAL -> GREATER_THAN;
                case EQUAL -> NOT_EQUAL;
                case NOT_EQUAL -> EQUAL;
            };
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
