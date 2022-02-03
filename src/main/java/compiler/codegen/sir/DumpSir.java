package compiler.codegen.sir;

import compiler.codegen.Register;
import compiler.codegen.sir.instructions.*;

import java.io.PrintWriter;
import java.util.HashSet;

public class DumpSir {
    private final PrintWriter out;
    private final SirGraph sirGraph;

    private final HashSet<BasicBlock> visited;

    private boolean withBlockSchedule;
    private boolean withInstructionIndices;

    public DumpSir(PrintWriter out, SirGraph sirGraph) {
        this.out = out;
        this.sirGraph = sirGraph;
        this.visited = new HashSet<>();
    }

    public DumpSir withBlockSchedule(boolean b) {
        this.withBlockSchedule = b;
        return this;
    }

    public DumpSir withInstructionIndices(boolean b) {
        this.withInstructionIndices = b;
        return this;
    }

    private String formatInstruction(Instruction instr) {
        return switch (instr) {
            case DivInstruction div -> String.format("%s <- %s %s %s", div.getTarget(), div.getMnemonic(), div.getDividend(), div.getDivisor());
            case BinaryInstruction binary -> String.format("%s <- %s %s %s", binary.getTarget(), binary.getMnemonic(), binary.getLhs(), binary.getRhs().formatIntelSyntax());
            case ShiftInstruction shift -> String.format("%s <- %s %s %s", shift.getTarget(), shift.getMnemonic(), shift.getLhs(), shift.getRhs().formatIntelSyntax());
            case AllocCallInstruction alloc -> String.format("%s <- %s (%s %s)", alloc.getTarget(), alloc.getMnemonic(), alloc.getNumElements(), alloc.getObjectSize());
            case BranchInstruction branch -> String.format("%s", branch.getMnemonic());
            case CmpInstruction cmp -> String.format("%s %s %s", cmp.getMnemonic(), cmp.getLhs(), cmp.getRhs().formatIntelSyntax());
            case JumpInstruction jump -> String.format("%s", jump.getMnemonic());
            case MethodCallInstruction method -> {
                yield String.format("%s <- %s %s (%s)", method.getTarget(), method.getMnemonic(), method.getMethod().getLinkerName(), method.getArguments());
            }
            case MovInstruction mov -> String.format("%s <- %s %s", mov.getDestination().formatIntelSyntax(), mov.getMnemonic(),  mov.getSource().formatIntelSyntax());
            case MovSignExtendInstruction movSX -> String.format("%s <- %s %s", movSX.getTarget(), movSX.getMnemonic(), movSX.getInput());
            case ReturnInstruction ret -> String.format("%s %s", ret.getMnemonic(), ret.getReturnValue().map(Register::toString).orElse(""));
            case PushInstruction push -> String.format("%s %s", push.getMnemonic(), push.getRegister());
            case PopInstruction pop -> String.format("%s <- %s", pop.getRegister(), pop.getMnemonic());
            case LeaveInstruction leave -> String.format("%s", leave.getMnemonic());
            case ConvertDoubleToQuadInstruction cdq -> String.format("%s <- %s %s", cdq.getTarget(), cdq.getMnemonic(), cdq.getDoubleWord());
            case LoadEffectiveAddressInstruction lea -> String.format("%s <- %s %s", lea.getTarget(), lea.getMnemonic(), lea.getLoc().formatIntelSyntax());
        };
    }
    private void printTarget(BasicBlock start, BasicBlock end, String label) {
        var constraint = this.withBlockSchedule ? ", constraint=false" : "";
        this.out.format("%s -> %s [label=\"%s\"%s];\n", start.getLabel(), end.getLabel(), label, constraint);
    }

    private void dumpBasicBlock(BasicBlock bb, int startInstructionIndex) {
        if (this.visited.contains(bb)) {
            return;
        } else {
            this.visited.add(bb);
        }

        StringBuilder label = new StringBuilder();
        label.append(String.format("%s\\l", bb.getLabel()));

        int instructionIndex = startInstructionIndex;
        for (var instruction : bb.getInstructions()) {
            var index = this.withInstructionIndices ? String.format("%04d ", instructionIndex) : "";
            label.append(String.format("%s%s\\l", index, this.formatInstruction(instruction)));
            instructionIndex += 1;
        }

        this.out.format("%s [label=\"%s\", shape=rectangle];\n", bb.getLabel(), label);

        switch (bb.getLastInstruction()) {
            case JumpInstruction jump -> {
                this.printTarget(bb, jump.getTarget(), "");
            }
            case BranchInstruction branch -> {
                this.printTarget(bb, branch.getTrueBlock(), "true");
                this.printTarget(bb, branch.getFalseBlock(), "false");
            }
            case ReturnInstruction ignored -> {}
        }
    }

    public void dump() {
        this.out.format("digraph {\n");

        for (int i = 0; i < this.sirGraph.getBlocks().size(); i++) {
            var bb = this.sirGraph.getBlocks().get(i);
            var startInstructionIndex = this.sirGraph.getStartInstructionIndices().get(i);
            this.dumpBasicBlock(bb, startInstructionIndex);
        }

        if (this.withBlockSchedule) {
            var blockSchedule = this.sirGraph.getBlocks();
            for (int i = 0; i < blockSchedule.size() - 1; i++) {
                var startBlock = blockSchedule.get(i);
                var targetBlock = blockSchedule.get(i + 1);

                this.out.format("\t%s -> %s [style=invis, constraint=true]\n", startBlock.getLabel(), targetBlock.getLabel());
            }
        }

        this.out.format("}\n");
        this.out.flush();
    }
}
