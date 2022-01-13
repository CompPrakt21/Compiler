package compiler.codegen.sir.instructions;

import compiler.codegen.Predicate;
import compiler.codegen.Register;
import compiler.codegen.sir.BasicBlock;

import java.util.List;
import java.util.Optional;

public final class BranchInstruction extends ControlFlowInstruction {

    private Predicate predicate;
    private BasicBlock trueBlock;
    private BasicBlock falseBlock;

    public BranchInstruction(Predicate predicate, BasicBlock trueBlock, BasicBlock falseBlock) {
        this.predicate = predicate;
        this.trueBlock = trueBlock;
        this.falseBlock = falseBlock;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public BasicBlock getTrueBlock() {
        return trueBlock;
    }

    public BasicBlock getFalseBlock() {
        return falseBlock;
    }

    public void setTrueBlock(BasicBlock trueBlock) {
        this.trueBlock = trueBlock;
    }

    public void setFalseBlock(BasicBlock falseBlock) {
        this.falseBlock = falseBlock;
    }

    @Override
    public String getMnemonic() {
        return String.format("b%s", this.predicate.getSuffix());
    }

    @Override
    public List<Register> getReadRegisters() {
        return List.of();
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        return Optional.empty();
    }
}
