package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.Instruction;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegisterLifetimes {
    /**
     * Stores at what instruction, which virtual registers are live.
     *
     * A register is considered live at an instruction if two things are true:
     * 1. The current instruction, or an instruction before writes to the register.
     * 2. A following instruction (not including the current instruction) reads from the register.
     *
     * The list corresponds with the list of instructions at stores which virtual registers are life
     * at each instruction.
     */
    private final Map<BasicBlock, List<Set<VirtualRegister>>> lifetimes;

    /**
     * If a virtual register appears in multiple different basic blocks.
     * They are considered always live
     */
    private final Set<VirtualRegister> alwaysLive;

    private Set<VirtualRegister> registersInOtherBlocks;

    private RegisterLifetimes() {
        this.alwaysLive = new HashSet<>();
        this.lifetimes = new HashMap<>();
        this.registersInOtherBlocks = new HashSet<>();
    }

    private void calculateLifetimesOfBasicBlock(BasicBlock bb) {
        var instructions = bb.getInstructions();

        Set<VirtualRegister> registersInThisBlock = new HashSet<>();

        List<Set<VirtualRegister>> lifetimes = new ArrayList<>();

        Set<VirtualRegister> currentLiveRegisters = new HashSet<>();

        for (int i = instructions.size() - 1; i >= 0; i--) {

            lifetimes.add(new HashSet<>(currentLiveRegisters));

            var instruction = instructions.get(i);

            var writtenRegister = instruction.getWrittenRegister();
            writtenRegister.ifPresent(reg -> currentLiveRegisters.remove((VirtualRegister) reg));

            writtenRegister.ifPresent(reg -> registersInThisBlock.add((VirtualRegister) reg));

            var readRegisters = instruction.getReadRegisters().stream().map(reg -> (VirtualRegister) reg).toList();
            currentLiveRegisters.addAll(readRegisters);
            registersInThisBlock.addAll(readRegisters);
        }

        Set<VirtualRegister> copy = new HashSet<>(registersInThisBlock);
        copy.retainAll(this.registersInOtherBlocks);
        this.alwaysLive.addAll(copy);

        this.registersInOtherBlocks.addAll(registersInThisBlock);

        Collections.reverse(lifetimes);

        this.lifetimes.put(bb, lifetimes);
    }

    public Set<VirtualRegister> getLiveRegisters(BasicBlock bb, int instructionIndex) {
        return Stream.concat(this.lifetimes.get(bb).get(instructionIndex).stream(), this.alwaysLive.stream()).collect(Collectors.toSet());
    }

    public static RegisterLifetimes calculateLifetime(SirGraph graph) {
        var lifetimes = new RegisterLifetimes();

        for (var bb : graph.getBlocks()) {
            lifetimes.calculateLifetimesOfBasicBlock(bb);
        }

        // This is no longer needed!
        lifetimes.registersInOtherBlocks = null;

        return lifetimes;
    }
}
