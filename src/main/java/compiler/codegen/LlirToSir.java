package compiler.codegen;

import compiler.codegen.llir.BasicBlock;
import compiler.codegen.llir.LlirGraph;
import compiler.codegen.llir.nodes.LlirNode;
import compiler.codegen.llir.nodes.RegisterNode;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LlirToSir {
    private final LlirGraph llirGraph;
    private final ScheduleResult schedule;

    private final Map<compiler.codegen.llir.BasicBlock, compiler.codegen.sir.BasicBlock> blockMap;

    public LlirToSir(LlirGraph llirGraph, ScheduleResult schedule) {
        this.llirGraph = llirGraph;
        this.schedule = schedule;

        this.blockMap = new HashMap<>();
    }

    public SirGraph transform() {
        for (var pair : this.schedule.schedule().entrySet()) {
            var bb = pair.getKey();
            var instructions = pair.getValue();

            this.transformBasicBlock(bb, instructions);
        }

        // Connect control flow edges.
        for (var bb : this.schedule.schedule().keySet()) {

            var sirBb = this.blockMap.get(bb);

            var lastInstruction = sirBb.getInstructions().get(sirBb.getInstructions().size() - 1);
            var controlFlowInstruction = (ControlFlowInstruction) lastInstruction;

            var llirEndNode = bb.getEndNode();

            switch (controlFlowInstruction) {
                case JumpInstruction jump -> {
                    var llirJump = (compiler.codegen.llir.nodes.JumpInstruction) llirEndNode;
                    var llirTarget = llirJump.getTarget();
                    var sirTarget = this.blockMap.get(llirTarget);
                    jump.setTarget(sirTarget);
                }
                case BranchInstruction branch -> {
                    var llirBranch = (compiler.codegen.llir.nodes.BranchInstruction) llirEndNode;
                    var llirTrueBlock = llirBranch.getTrueBlock();
                    var llirFalseBlock = llirBranch.getFalseBlock();

                    var sirTrueBlock = this.blockMap.get(llirTrueBlock);
                    var sirFalseBlock = this.blockMap.get(llirFalseBlock);

                    branch.setTrueBlock(sirTrueBlock);
                    branch.setFalseBlock(sirFalseBlock);
                }
                case ReturnInstruction ignored -> {}
                default -> throw new AssertionError("Im definitely covering all cases, but java doesnt believe me");
            }
        }

        var blocks = this.blockMap.values().stream().toList();

        return new SirGraph(this.blockMap.get(this.llirGraph.getStartBlock()), blocks);
    }

    private Instruction transformInstruction(LlirNode node) {
        return switch (node) {
            case compiler.codegen.llir.nodes.AllocCallInstruction alloc -> new AllocCallInstruction(alloc.getTargetRegister(), alloc.getElemSize().getTargetRegister(), alloc.getNumElements().getTargetRegister());
            // Control flow edges are resolved at the end.
            case compiler.codegen.llir.nodes.BranchInstruction branch ->
                    new BranchInstruction(branch.getPredicate(), null, null);
            case compiler.codegen.llir.nodes.CmpInstruction cmp -> new CmpInstruction(cmp.getLhs().getTargetRegister(), cmp.getRhs().getTargetRegister());
            case compiler.codegen.llir.nodes.DivInstruction div -> new DivInstruction(div.getTargetRegister(), div.getDividend().getTargetRegister(), div.getDivisor().getTargetRegister(), switch (div.getType()) {
                case Div -> DivInstruction.DivType.Div;
                case Mod -> DivInstruction.DivType.Mod;
            });
            case compiler.codegen.llir.nodes.InputNode ignored -> throw new IllegalArgumentException("Input node not represented in SIR");
            case compiler.codegen.llir.nodes.JumpInstruction ignored -> new JumpInstruction(null);
            case compiler.codegen.llir.nodes.MemoryInputNode ignored -> throw new IllegalArgumentException("Memory input node not represented in SIR");
            case compiler.codegen.llir.nodes.MethodCallInstruction method -> {
                var arguments = method.getArguments().stream().map(RegisterNode::getTargetRegister).toList();
                yield new MethodCallInstruction(method.getTargetRegister(), method.getCalledMethod(), arguments);
            }
            case compiler.codegen.llir.nodes.MovImmediateInstruction movImm -> new MovImmediateInstruction(movImm.getTargetRegister(), movImm.getImmediateValue());
            case compiler.codegen.llir.nodes.MovLoadInstruction movLoad -> new MovLoadInstruction(movLoad.getTargetRegister(), new MemoryLocation(movLoad.getAddrNode().getTargetRegister()));
            case compiler.codegen.llir.nodes.MovRegisterInstruction movReg -> new MovRegInstruction(movReg.getTargetRegister(), movReg.getSourceRegister().getTargetRegister());
            case compiler.codegen.llir.nodes.MovSignExtendInstruction movSX -> new MovSignExtendInstruction(movSX.getTargetRegister(), movSX.getInput().getTargetRegister());
            case compiler.codegen.llir.nodes.MovStoreInstruction movStore -> new MovStoreInstruction(new MemoryLocation(movStore.getAddrNode().getTargetRegister()), movStore.getValueNode().getTargetRegister());
            case compiler.codegen.llir.nodes.ReturnInstruction ret -> new ReturnInstruction(ret.getReturnValue().map(RegisterNode::getTargetRegister));
            case compiler.codegen.llir.nodes.AddInstruction add -> new AddInstruction(add.getTargetRegister(), add.getLhs().getTargetRegister(), add.getRhs().getTargetRegister());
            case compiler.codegen.llir.nodes.SubInstruction sub -> new SubInstruction(sub.getTargetRegister(), sub.getLhs().getTargetRegister(), sub.getRhs().getTargetRegister());
            case compiler.codegen.llir.nodes.MulInstruction mul -> new MulInstruction(mul.getTargetRegister(), mul.getLhs().getTargetRegister(), mul.getRhs().getTargetRegister());
            case compiler.codegen.llir.nodes.XorInstruction xor -> new XorInstruction(xor.getTargetRegister(), xor.getLhs().getTargetRegister(), xor.getRhs().getTargetRegister());
        };
    }

    private void transformBasicBlock(BasicBlock bb, List<LlirNode> instructions) {
        var sirInstructions = instructions.stream().map(this::transformInstruction).toList();
        var sirBb = new compiler.codegen.sir.BasicBlock(bb.getLabel(), sirInstructions);

        this.blockMap.put(bb, sirBb);
    }
}
