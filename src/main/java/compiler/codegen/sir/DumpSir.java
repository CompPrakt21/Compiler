package compiler.codegen.sir;

import compiler.codegen.sir.instructions.*;

import java.io.PrintWriter;
import java.util.HashSet;

public class DumpSir {
    private final PrintWriter out;
    private final SirGraph sirGraph;

    private final HashSet<BasicBlock> visited;

    public DumpSir(PrintWriter out, SirGraph sirGraph) {
        this.out = out;
        this.sirGraph = sirGraph;
        this.visited = new HashSet<>();
    }

    private String formatInstruction(Instruction instr) {
        return switch (instr) {
            case BinaryInstruction binary -> String.format("%s <- %s %s %s", binary.getTarget(), binary.getMnemonic(), binary.getLhs(), binary.getRhs());
            case AllocCallInstruction alloc -> String.format("%s <- %s <alloc> (%s %s)", alloc.getTarget(), alloc.getMnemonic(), alloc.getNumElements(), alloc.getObjectSize());
            case BranchInstruction branch -> String.format("%s", branch.getMnemonic());
            case CmpInstruction cmp -> String.format("%s %s %s", cmp.getMnemonic(), cmp.getLhs(), cmp.getRhs());
            case JumpInstruction jump -> String.format("%s", jump.getMnemonic());
            case MethodCallInstruction method -> {
                yield String.format("%s <- %s %s (%s)", method.getTarget(), method.getMnemonic(), method.getMethod().getLinkerName(), method.getArguments());
            }
            case MovImmediateInstruction movImm -> String.format("%s <- %s %s", movImm.getTarget(), movImm.getMnemonic(), movImm.getImmediateValue());
            case MovLoadInstruction movLoad -> String.format("%s <- %s [%s]", movLoad.getTarget(), movLoad.getMnemonic(), movLoad.getAddress());
            case MovRegInstruction movReg -> String.format("%s <- %s %s", movReg.getTarget(), movReg.getMnemonic(), movReg.getSource());
            case MovSignExtendInstruction movSX -> String.format("%s <- %s %s", movSX.getTarget(), movSX.getMnemonic(), movSX.getInput());
            case MovStoreInstruction movStore -> String.format("%s [%s] %s", movStore.getMnemonic(), movStore.getAddress(), movStore.getValue());
            case ReturnInstruction ret -> String.format("%s", ret.getMnemonic());
        };
    }
    private void printTarget(BasicBlock start, BasicBlock end, String label) {
        this.out.format("%s -> %s [label=\"%s\"];\n", start.getLabel(), end.getLabel(), label);
    }

    private void dumpBasicBlock(BasicBlock bb) {
        if (this.visited.contains(bb)) {
            return;
        } else {
            this.visited.add(bb);
        }

        StringBuilder label = new StringBuilder();
        label.append(String.format("%s\\l", bb.getLabel()));

        for (var instruction : bb.getInstructions()) {
            label.append(String.format("%s\\l", this.formatInstruction(instruction)));
        }

        this.out.format("%s [label=\"%s\", shape=rectangle];\n", bb.getLabel(), label);

        var controlFlowInstruction = (ControlFlowInstruction)bb.getInstructions().get(bb.getInstructions().size() - 1);

        switch (controlFlowInstruction) {
            case JumpInstruction jump -> {
                this.dumpBasicBlock(jump.getTarget());
                this.printTarget(bb, jump.getTarget(), "");
            }
            case BranchInstruction branch -> {
                this.dumpBasicBlock(branch.getTrueBlock());
                this.dumpBasicBlock(branch.getFalseBlock());
                this.printTarget(bb, branch.getTrueBlock(), "true");
                this.printTarget(bb, branch.getFalseBlock(), "false");
            }
            case ReturnInstruction ignored -> {}
        }
    }

    public void dump() {
        this.out.format("digraph {\n");

        this.dumpBasicBlock(this.sirGraph.getStartBlock());

        this.out.format("}\n");
        this.out.flush();
    }
}
