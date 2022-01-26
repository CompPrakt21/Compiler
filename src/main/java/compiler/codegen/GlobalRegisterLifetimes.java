package compiler.codegen;

import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.ControlFlowInstruction;

import java.util.*;
import java.util.stream.Collectors;

public class GlobalRegisterLifetimes {
    private final SirGraph graph;

    /**
     * Does this block have incoming backedges (edges from blocks that are scheduled later than this block)
     */
    private final Map<BasicBlock, Boolean> incomingBackEdge;

    /**
     * Which position has this basic block in the block schedule.
     */
    private final Map<BasicBlock, Integer> scheduleIndex;

    /**
     * We need to extend the lifetimes of virtual registers that are used within a loops, whose definition is before the loop, until the end of the loop.
     * Loops are detected using backedges in the linearized control flow graph.
     * Therefore we need to keep track of the earliest definition and last use of a register, as well as the loops they are contained within.
     *
     * A loop is represented as the start and end instruction point.
     * Since loops can be nested an instruction can be within multiple loops so we store the stack of active loops.
     * Loops that are earlier in the stack are larger.
     */
    private record InstructionPoint(int instructionIndex, Stack<Interval> activeLoops) {}
    private final Map<VirtualRegister, InstructionPoint> earliestDefinition;
    private final Map<VirtualRegister, InstructionPoint> lastUse;

    /**
     * Stores the live range interval of each virtual register.
     */
    private final Map<VirtualRegister, Interval> liveIntervals;

    public static class Lifetimes {
        private final Map<VirtualRegister, Interval> liveIntervals;

        private Lifetimes(Map<VirtualRegister, Interval> liveIntervals) {
            this.liveIntervals = liveIntervals;
        }

        public Set<VirtualRegister> getLiveRegisters(int instructionIndex) {
            return this.liveIntervals.keySet().stream()
                    .filter(virtReg -> this.liveIntervals.get(virtReg).contains(instructionIndex))
                    .collect(Collectors.toSet());
        }

        public String toString() {
            StringBuilder result = new StringBuilder();

            var regs = new ArrayList<>(this.liveIntervals.keySet());
            regs.sort(Comparator.comparing(VirtualRegister::getName));
            for (var reg : regs) {
                var interval = this.liveIntervals.get(reg);
                result.append(String.format("%s: [%s, %s]\n", reg.getName(), interval.getStart(), interval.getStop()));
            }

            return result.toString();
        }
    }

    /**
     * Represents a live variable range.
     * The start and stop values specify the instruction index (see startInstructionIndex).
     */
    public static class Interval {
        private int start;
        private int stop;

        public Interval(int start, int stop) {
            this.start = start;
            this.stop = stop;
        }

        public boolean contains(int x) {
            return start <= x && x <= stop;
        }

        public int getStart() {
            return start;
        }

        public int getStop() {
            return stop;
        }
    }

    private GlobalRegisterLifetimes(SirGraph graph) {
        this.graph = graph;
        this.scheduleIndex = new HashMap<>();
        this.incomingBackEdge = new HashMap<>();
        this.earliestDefinition = new HashMap<>();
        this.lastUse = new HashMap<>();
        this.liveIntervals = new HashMap<>();
    }

    private boolean hasOutgoingBackedge(BasicBlock bb) {
        var index = this.scheduleIndex.get(bb);
        return bb.getLastInstruction().getTargets().stream().anyMatch(targetBb -> this.scheduleIndex.get(targetBb) <= index);
    }

    private void calculateLifetimes() {
        BlockSchedule.scheduleReversePostorder(this.graph);
        var schedule = this.graph.getBlocks();

        for (int i = 0; i < schedule.size(); i++) {
            this.scheduleIndex.put(schedule.get(i), i);
        }

        for (var bb : schedule) {
            this.incomingBackEdge.putIfAbsent(bb, false);

            var lastInstr = (ControlFlowInstruction) bb.getInstructions().get(bb.getInstructions().size() - 1);
            for (var target : lastInstr.getTargets()) {
                if (this.scheduleIndex.get(target) <= this.scheduleIndex.get(bb)) {
                    this.incomingBackEdge.put(target, true);
                }
            }
        }

        Set<VirtualRegister> liveRegisters = new HashSet<>();

        var lastBb = schedule.get(schedule.size() - 1);
        var currentInstructionIndex = this.graph.getStartInstructionIndices().get(this.graph.getStartInstructionIndices().size() - 1) + lastBb.getInstructions().size() - 1;
        var activeLoops = new Stack<Interval>();
        for (int blockIdx = schedule.size() - 1; blockIdx >= 0; blockIdx--) {
            var bb = schedule.get(blockIdx);

            if (hasOutgoingBackedge(bb)) {
                activeLoops.push(new Interval(-10, currentInstructionIndex));
            }

            for (int instructionInBlockIdx = bb.getInstructions().size() - 1; instructionInBlockIdx >= 0; instructionInBlockIdx--) {
                var instruction = bb.getInstructions().get(instructionInBlockIdx);

                // Which registers are live that weren't live before this instruction.
                var newLiveRegs = new HashSet<>(instruction.getReadRegisters().stream().map(reg -> (VirtualRegister) reg).toList());
                newLiveRegs.removeAll(liveRegisters);

                liveRegisters.addAll(newLiveRegs);

                for (var liveReg : newLiveRegs) {
                    if (!this.lastUse.containsKey(liveReg)) {
                        this.liveIntervals.put(liveReg, new Interval(-10, currentInstructionIndex - 1));
                        this.lastUse.put(liveReg, new InstructionPoint(currentInstructionIndex, (Stack<Interval>) activeLoops.clone()));
                    }
                }

                var writtenReg = instruction.getWrittenRegister();
                if (writtenReg.isPresent()) {
                    var reg = (VirtualRegister) writtenReg.get();
                    this.earliestDefinition.put(reg, new InstructionPoint(currentInstructionIndex, (Stack<Interval>) activeLoops.clone()));

                    var regInterval = this.liveIntervals.get(reg);
                    // if reg is never used or the second mov of a phi a read might never been encountered.
                    if (regInterval != null) {
                        regInterval.start = currentInstructionIndex;
                    }
                    liveRegisters.remove(reg);
                }

                currentInstructionIndex -= 1;
            }

            if (incomingBackEdge.get(bb)) {
                var loop = activeLoops.pop();
                loop.start = Math.max(loop.start, currentInstructionIndex + 1); // +1 because we already decremented the currentInstructionIndex.
            }
        }
        assert currentInstructionIndex == -1;

        // extends livetimes to the end of loops if necessary
        for (var pair : this.liveIntervals.entrySet()) {
            var virtReg = pair.getKey();
            var interval = pair.getValue();

            var earliestDef = this.earliestDefinition.get(virtReg);
            var lastUse= this.lastUse.get(virtReg);

            Optional<Interval> loop = Optional.empty();

            // We iterate over every loop around the last use from smallest to largest loop.
            // If the definition is not within the loop, the loop has to start after the definition
            // and the lifetime of the register has to be extended to the end of the loop.
            for (int loopIdx = lastUse.activeLoops.size() - 1; loopIdx >= 0; loopIdx--) {
                var loopAroundLastUse = lastUse.activeLoops.get(loopIdx);

                // If there is no earliestDef, then virtReg contains a function argument which is never defined within any loop.
                if (earliestDef == null || !earliestDef.activeLoops.contains(loopAroundLastUse)) {
                    loop = Optional.of(loopAroundLastUse);
                }
            }

            loop.ifPresent(l -> interval.stop = l.stop);
        }
    }

    public static Lifetimes calculateLifetimes(SirGraph graph) {
        var lifetimes = new GlobalRegisterLifetimes(graph);

        lifetimes.calculateLifetimes();

        return new Lifetimes(lifetimes.liveIntervals);
    }
}
