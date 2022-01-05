package compiler.codegen.llir;

import compiler.codegen.ScheduleResult;
import compiler.codegen.llir.nodes.*;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.stream.Collectors;

public class DumpScheduledLlir {
    private final PrintWriter out;

    private LlirGraph graph;
    private ScheduleResult schedule;

    private final HashSet<BasicBlock> visitedBlocks;

    public DumpScheduledLlir(PrintWriter out) {
        this.out = out;
        this.visitedBlocks = new HashSet<>();
    }

    private String formatNode(LlirNode node) {
        return switch (node) {
            case BinaryInstruction binary -> String.format("%s <- %s %s %s", binary.getTargetRegister(), binary.getMnemonic(), binary.getLhs().getTargetRegister(), binary.getRhs().getTargetRegister());
            case AllocCallInstruction alloc -> String.format("%s <- %s <alloc> (%s %s)", alloc.getTargetRegister(), alloc.getMnemonic(), alloc.getNumElements().getTargetRegister(), alloc.getElemSize().getTargetRegister());
            case BranchInstruction branch -> String.format("%s", branch.getMnemonic());
            case CmpInstruction cmp -> String.format("%s %s %s", cmp.getMnemonic(), cmp.getLhs().getTargetRegister(), cmp.getRhs().getTargetRegister());
            case DivInstruction div -> String.format("%s <- %s %s %s", div.getTargetRegister(), div.getMnemonic(), div.getDividend().getTargetRegister(), div.getDivisor().getTargetRegister());
            case InputNode ignored -> throw new IllegalArgumentException("Input nodes should not be scheduled.");
            case JumpInstruction jump -> String.format("%s", jump.getMnemonic());
            case MemoryInputNode ignored -> throw new IllegalArgumentException("Memory input nodes should not be scheduled.");
            case MethodCallInstruction method -> {
                var arguments = method.getArguments().stream().map(RegisterNode::getTargetRegister).collect(Collectors.toList());
                yield String.format("%s <- %s %s (%s)", method.getTargetRegister(), method.getMnemonic(), method.getCalledMethod().getLinkerName(), arguments);
            }
            case MovImmediateInstruction movImm -> String.format("%s <- %s %s", movImm.getTargetRegister(), movImm.getMnemonic(), movImm.getImmediateValue());
            case MovLoadInstruction movLoad -> String.format("%s <- %s [%s]", movLoad.getTargetRegister(), movLoad.getMnemonic(), movLoad.getAddrNode().getTargetRegister());
            case MovRegisterInstruction movReg -> String.format("%s <- %s %s", movReg.getTargetRegister(), movReg.getMnemonic(), movReg.getSourceRegister().getTargetRegister());
            case MovSignExtendInstruction movSX -> String.format("%s <- %s %s", movSX.getTargetRegister(), movSX.getMnemonic(), movSX.getInput().getTargetRegister());
            case MovStoreInstruction movStore -> String.format("%s [%s] %s", movStore.getMnemonic(), movStore.getAddrNode().getTargetRegister(), movStore.getValueNode().getTargetRegister());
            case ReturnInstruction ret -> String.format("%s", ret.getMnemonic());
        };
    }

    private void printTarget(BasicBlock start, BasicBlock end, String label) {
        this.out.format("%s -> %s [label=\"%s\"];\n", start.getLabel(), end.getLabel(), label);
    }

    private void dumpBasicBlock(BasicBlock bb) {
        if (this.visitedBlocks.contains(bb)) {
            return;
        } else {
            this.visitedBlocks.add(bb);
        }

        StringBuilder label = new StringBuilder();
        label.append(String.format("%s\\l", bb.getLabel()));

        var schedule = this.schedule.schedule().get(bb);

        for (var instruction : schedule) {
            label.append(String.format("%s\\l", this.formatNode(instruction)));
        }

        this.out.format("%s [label=\"%s\", shape=rectangle];\n", bb.getLabel(), label);

        switch (bb.getEndNode()) {
            case JumpInstruction jump -> this.printTarget(bb, jump.getTarget(), "");
            case BranchInstruction branch -> {
                this.printTarget(bb, branch.getTrueBlock(), "true");
                this.printTarget(bb, branch.getFalseBlock(), "false");
            }
            case ReturnInstruction ignored -> {}
        }

        for (var successor : bb.getEndNode().getTargets()) {
            this.dumpBasicBlock(successor);
        }
    }

    public void dump(LlirGraph graph, ScheduleResult schedule) {
        this.graph = graph;
        this.schedule = schedule;

        this.visitedBlocks.clear();

        this.out.println("digraph {");

        this.dumpBasicBlock(this.graph.getStartBlock());

        this.out.println("}");
        this.out.flush();
    }
}
