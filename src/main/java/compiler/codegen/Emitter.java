package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.instructions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Emitter {

    private StringBuilder builder;
    private String currenFuncName;

    public Emitter() {
        this.builder = new StringBuilder();
        this.append(".text\n\n");
    }

    private void append(String string) {
        this.builder.append(string);
        this.builder.append("\n");
    }

    private String makeLabel(BasicBlock block) {
        return currenFuncName + block.getLabel();
    }

    /* TODO: Add function information */
    public void beginFunction(String name, int numArgs, boolean isVoid) {
        this.append(String.format(".globl\t%s", name));
        this.append(String.format(".type\t%s, @function", name));
        this.append(name +  ":");

        this.currenFuncName = name;
    }

    public void beginBlock(BasicBlock block) {
        this.append(makeLabel(block) + ":");
    }

    public void endBlock() {

    }


    public void endFunction() {

    }

    public void emitInstruction(Instruction instruction) {
        String asm;
        switch (instruction) {
            case AddInstruction insn -> {
                asm = String.format("\tadd %s, %s",
                        insn.getRhs().formatATTSyntax(), insn.getLhs().formatATTSyntax());
            }
            case AllocCallInstruction insn -> {
                asm = String.format("\tcall %s", "malloc");
            }
            case BranchInstruction insn -> {
                asm = String.format("\tj%s %s",
                        insn.getPredicate().getSuffix(),
                        makeLabel(insn.getTrueBlock()));
                asm += String.format("\n\tjmp %s", makeLabel(insn.getFalseBlock()));
            }
            case CmpInstruction insn -> {
                // AT&T reverses operands...
                asm = String.format("\tcmp %s, %s",
                        insn.getRhs().formatATTSyntax(),
                        insn.getLhs().formatATTSyntax());
            }
            case DivInstruction insn -> {
                // TODO: This is wrong, molki needs you to pass two result registers
                //asm = String.format("\tidiv %s", insn.
                throw new UnsupportedOperationException("Div not implemented");
            }
            case JumpInstruction insn -> {
                asm = String.format("\tjmp %s",
                        makeLabel(insn.getTarget()));
            }
            case LeaveInstruction insn -> {
                asm = "\tleave";
            }
            case MethodCallInstruction insn -> {
                asm = String.format("\tcall %s", insn.getMethod().getLinkerName());
            }
            case MovImmediateInstruction insn -> {
                asm = String.format("\tmov $%d, %s",
                        insn.getImmediateValue(),
                        insn.getTarget().formatATTSyntax());

            }
            case MovLoadInstruction insn -> {
                asm = String.format("\tmov %s, %s",
                        insn.getAddress().formatATTSyntax(),
                        insn.getTarget().formatATTSyntax());
            }
            case MovRegInstruction insn -> {
                asm = String.format("\tmov %s, %s",
                        insn.getSource().formatATTSyntax(),
                        insn.getTarget().formatATTSyntax());
            }
            case MovSignExtendInstruction insn -> {
                asm = String.format("\tmovsx %s, %s",
                        insn.getInput().formatATTSyntax(),
                        insn.getTarget().formatATTSyntax());
            }
            case MovStoreInstruction insn -> {
                asm = String.format("\tmov %s, %s",
                        insn.getValue().formatATTSyntax(),
                        insn.getAddress().formatATTSyntax());
            }
            case MulInstruction insn -> {
                // We use mul here as molki always uses 64 bits >:(
                // For the AssemblyEmitter this should be mull
                asm = String.format("\timul %s, %s",
                        insn.getRhs().formatATTSyntax(),
                        insn.getLhs().formatATTSyntax());
            }
            case PopInstruction insn -> {
                asm = String.format("\tpop %s",
                        insn.getRegister().formatATTSyntax());
            }
            case PushInstruction insn -> {
                asm = String.format("\tpush %s",
                        insn.getRegister().formatATTSyntax());
            }
            case ReturnInstruction insn -> {
                asm = "\tret";
            }
            case SubInstruction insn -> {
                asm = String.format("\tsub %s, %s",
                        insn.getRhs().formatATTSyntax(),
                        insn.getLhs().formatATTSyntax());
            }
            case XorInstruction insn -> {
                asm = String.format("\txor %s, %s",
                        insn.getRhs().formatATTSyntax(),
                        insn.getLhs().formatATTSyntax());

            }
            default -> throw new IllegalArgumentException("Instruction not emitable: " + instruction);
        }

        this.append(asm);
    }

    public void write(File asmFile) throws IOException {
        Files.write(asmFile.toPath(), List.of(builder.toString()));
    }
}
