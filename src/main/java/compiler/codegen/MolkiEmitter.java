package compiler.codegen;

import compiler.codegen.llir.*;

public class MolkiEmitter extends Emitter {

    public MolkiEmitter() {
        super();
    }

    @Override
    public void beginFunction(String name, int numArgs, boolean isVoid) {
        this.append(String.format(".function %s %d %d", name, numArgs, isVoid ? 0 : 1));
    }

    @Override
    public void endFunction() {
        this.append(".endfunction");
    }

    @Override
    public void beginBlock(BasicBlock block) {
        this.append(block.getLabel() + ":");
    }

    @Override
    public void endBlock(ControlFlowNode endNode) {
        // Do nothing for now, might be necessary elsewhere
    }

    @Override
    public void emitInstruction(LlirNode node) {
        String asm;
        switch (node) {
            case AddInstruction insn -> {
                asm = String.format("\tadd [%%@%d | %%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case BranchInstruction insn -> {
                asm = String.format("\tj%s %s",
                        insn.getPredicate().getSuffix(),
                        insn.getTrueBlock().getLabel());
            }
            case CmpInstruction insn -> {
                asm = String.format("\tcmp [%%@%d | %%@%d]",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId());
            }
            case JumpInstruction insn -> {
                asm = String.format("\tjmp %s",
                        insn.getTarget().getLabel());
            }
            case MovImmediateInstruction insn -> {
                asm = String.format("\tmov $%d, %%@%d",
                        insn.getImmediateValue(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovLoadInstruction insn -> {
                asm = String.format("\tmov [%%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getAddrNode().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovRegisterInstruction insn -> {
                asm = String.format("\tmov [%%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getSourceRegister().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovStoreInstruction insn -> {
                asm = String.format("\tmov [%%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getValueNode().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getAddrNode().getTargetRegister()).getId());
            }
            case MulInstruction insn -> {
                asm = String.format("\tmull [%%@%d | %%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case ReturnInstruction insn -> {
                asm = "";//String.format("\tret %%@%d",
                       // ((VirtualRegister)insn.getReturnValue().orElseThrow().getTargetRegister()).getId());
            }
            default -> throw new IllegalArgumentException("Node not emitable: " + node);
        }

        this.append(asm);
    }
}
