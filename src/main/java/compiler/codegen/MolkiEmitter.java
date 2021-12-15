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
            case CallInstruction insn -> {
                asm = String.format("\tcall %s",
                        insn.getCalledMethod().getLinkerName());
            }
            case CmpInstruction insn -> {
                asm = String.format("\tcmp %%@%d, %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId());
            }
            case DivInstruction insn -> {
                // TODO: This is wrong, molki needs you to pass two result registers
                asm = String.format("\tidiv [%%@%d | %%@%d] -> [%%@%d | %%@%d]",
                        ((VirtualRegister)insn.getDividend().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getDivisor().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case JumpInstruction insn -> {
                asm = String.format("\tjmp %s",
                        insn.getTarget().getLabel());
            }
            case ModInstruction insn -> {
                // TODO: This is wrong, molki needs you to pass two result registers
                asm = String.format("\tidiv [%%@%d | %%@%d] -> [%%@%d | %%@%d]",
                        ((VirtualRegister)insn.getDividend().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getDivisor().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovImmediateInstruction insn -> {
                asm = String.format("\tmov $%d, %%@%d",
                        insn.getImmediateValue(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovLoadInstruction insn -> {
                asm = String.format("\tmov 0(%%@%d), %%@%d",
                        ((VirtualRegister)insn.getAddrNode().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovRegisterInstruction insn -> {
                asm = String.format("\tmov %%@%d, %%@%d",
                        ((VirtualRegister)insn.getSourceRegister().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case MovStoreInstruction insn -> {
                asm = String.format("\tmov %%@%d, 0(%%@%d)",
                        ((VirtualRegister)insn.getValueNode().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getAddrNode().getTargetRegister()).getId());
            }
            case MulInstruction insn -> {
                // We use mul here as molki always uses 64 bits >:(
                // For the AssemblyEmitter this should be mull
                asm = String.format("\tmul [%%@%d | %%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
            }
            case ReturnInstruction insn -> {
                if (insn.getReturnValue().isPresent()) {
                    asm = String.format("\tmov %%@%d, %%@r0",
                            ((VirtualRegister)insn.getReturnValue().get().getTargetRegister()).getId());
                } else {
                    return;
                }
            }
            case XorInstruction insn -> {
                asm = String.format("\txor [%%@%d | %%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());

            }
            default -> throw new IllegalArgumentException("Node not emitable: " + node);
        }

        this.append(asm);
    }
}
