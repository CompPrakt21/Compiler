package compiler.codegen;

import compiler.codegen.llir.*;
import compiler.codegen.llir.nodes.*;
import compiler.types.VoidTy;

public class MolkiEmitter extends Emitter {

    private VirtualRegister garbage;

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
        if (garbage == null) {
            garbage = block.getGraph().getVirtualRegGenerator().next32Register();
        }

        this.append(block.getLabel() + ":");
    }

    @Override
    public void endBlock() {
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
                asm += String.format("\n\tjmp %s", insn.getFalseBlock().getLabel());
            }
            case CallInstruction insn -> {
                boolean returnsNotVoid = switch (insn) {
                    case MethodCallInstruction m -> !(m.getCalledMethod().getReturnTy() instanceof VoidTy);
                    case AllocCallInstruction ignored -> true;
                };

                String calledName = switch (insn) {
                    case MethodCallInstruction m -> {
                        String n = m.getCalledMethod().getLinkerName();
                        yield switch (n) {
                            case "_System_out_println" -> "__stdlib_println";
                            case "_System_in_read" -> "__stdlib_read";
                            case "_System_out_write" -> "__stdlib_write";
                            case "_System_out_flush" -> "__stdlib_flush";
                            default -> n;
                        };
                    }
                    case AllocCallInstruction ignored -> "__stdlib_calloc";
                };

                var args = insn.getArguments();

                String argString;

                if (args.isEmpty()) {
                    argString = "[ ]";
                } else {
                    argString = "[ ";

                    for (int i = 0; i < args.size() - 1; i++) {
                        var virtReg = (VirtualRegister) args.get(i).getTargetRegister();
                        argString += String.format("%%@%d | ", virtReg.getId());
                    }

                    var virtReg = (VirtualRegister) args.get(args.size() - 1).getTargetRegister();
                    argString += String.format("%%@%d", virtReg.getId());

                    argString += " ]";
                }

                String returnString;
                if (!returnsNotVoid) {
                    returnString = "";
                } else {
                    var returnVirtReg = (VirtualRegister) insn.getTargetRegister();
                    returnString = String.format(" -> %%@%d", returnVirtReg.getId());
                }

                asm = String.format("\tcall %s %s %s",
                        calledName, argString, returnString);
            }
            case CmpInstruction insn -> {
                // AT&T reverses operands...
                asm = String.format("\tcmp %%@%d, %%@%d",
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId());
            }
            case DivInstruction insn -> {
                // TODO: This is wrong, molki needs you to pass two result registers
                asm = String.format("\tidiv [%%@%d | %%@%d] -> [%%@%d | %%@%d]",
                        ((VirtualRegister)insn.getDividend().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getDivisor().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId(),
                        this.garbage.getId());
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
                asm = "";
                if (insn.getReturnValue().isPresent()) {
                    asm = String.format("\tmov %%@%d, %%@r0",
                            ((VirtualRegister)insn.getReturnValue().get().getTargetRegister()).getId());
                }
                asm += "\n\treturn";
            }
            case SubInstruction insn -> {
                asm = String.format("\tsub [%%@%d | %%@%d] -> %%@%d",
                        ((VirtualRegister)insn.getLhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getRhs().getTargetRegister()).getId(),
                        ((VirtualRegister)insn.getTargetRegister()).getId());
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
