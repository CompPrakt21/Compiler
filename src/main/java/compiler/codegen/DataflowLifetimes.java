package compiler.codegen;

import compiler.codegen.llir.nodes.MovLoadInstruction;
import compiler.codegen.sir.BasicBlock;
import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.CallInstruction;
import compiler.codegen.sir.instructions.DivInstruction;
import compiler.codegen.sir.instructions.MovInstruction;
import compiler.utils.ListUtils;
import firm.nodes.Div;

import java.util.*;
import java.util.stream.Collectors;

public class DataflowLifetimes {
    private final SirGraph graph;

    /**
     * Used to get back the original virtual register based on its unique id
     */
    private final List<VirtualRegister> virtualRegisters;

    /**
     * Stores the live range interval of each virtual register.
     */
    private final Map<BasicBlock, Map<VirtualRegister, SegmentInterval>> liveIntervals;

    private static class SegmentInterval {
        private List<Integer> intervalStarts;
        private List<Integer> intervalEnd;
        private boolean finished;

        public SegmentInterval() {
            this.intervalStarts = new ArrayList<>();
            this.intervalEnd = new ArrayList<>();
            this.finished = false;
        }

        public boolean contains(int x) {
            assert this.finished;
            var searchIndex = Collections.binarySearch(this.intervalStarts, x);

            int intervalIdx;
            if (searchIndex < 0) {
                intervalIdx = -(searchIndex + 1) - 1;
            } else {
                intervalIdx = searchIndex;
            }

            if (intervalIdx < 0) return false;

            int start = this.intervalStarts.get(intervalIdx);
            int end = this.intervalEnd.get(intervalIdx);

            return start <= x && x <= end;
        }

        public void finish() {
            this.finished = true;
            this.intervalStarts.sort(Comparator.naturalOrder());
            this.intervalEnd.sort(Comparator.naturalOrder());

            assert this.intervalStarts.size() == this.intervalEnd.size();
            for (int i = 1; i < this.intervalStarts.size(); i++) {
                assert this.intervalEnd.get(i - 1) <= this.intervalStarts.get(i);
            }
        }

        public void addSegment(int start, int end) {
            assert !this.finished;
            assert start <= end;
            this.intervalStarts.add(start);
            this.intervalEnd.add(end);
        }

        @Override
        public String toString() {
            this.intervalStarts.sort(Comparator.naturalOrder());
            this.intervalEnd.sort(Comparator.naturalOrder());

            assert this.intervalStarts.size() == this.intervalEnd.size();

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < this.intervalStarts.size(); i++) {
                result.append(String.format("[%s, %s], ", this.intervalStarts.get(i), this.intervalEnd.get(i)));
            }

            return result.toString();
        }
    }

    public static class Lifetimes {
        private final Map<BasicBlock, Map<VirtualRegister, SegmentInterval>> liveIntervals;

        private Lifetimes(Map<BasicBlock, Map<VirtualRegister, SegmentInterval>> liveIntervals) {
            this.liveIntervals = liveIntervals;
        }

        /**
         * @param block
         * @param i The instruction relative the block start.
         * @return The set of registers that are live at this point in the program.
         */
        public Set<VirtualRegister> getLiveRegisters(BasicBlock block, int i) {
            return this.liveIntervals.get(block)
                    .entrySet()
                    .stream()
                    .filter(pair -> {
                        var interval = pair.getValue();
                        return interval.contains(i);
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        }

        public boolean isLiveAtBeginningOf(VirtualRegister virtReg, BasicBlock block) {
            var interval = this.liveIntervals.get(block).get(virtReg);
            return interval != null && interval.contains(-1);
        }
    }

    private DataflowLifetimes(SirGraph graph) {
        this.graph = graph;
        this.liveIntervals = new HashMap<>();
        this.virtualRegisters = new ArrayList<>();
    }

    private record GenKill(BitSet gen, BitSet kill){}
    private GenKill calculateGenKillForBlock(BasicBlock block) {
        var gen = new BitSet();
        var kill = new BitSet();

        var instructions = block.getInstructions();

        for (int i = instructions.size() - 1; i >= 0; i--) {
            var instruction = instructions.get(i);

            instruction.getWrittenRegister().ifPresent(reg -> {
                var virtReg = (VirtualRegister) reg;
                kill.set(virtReg.getId(), true);
                gen.set(virtReg.getId(), false);

                ListUtils.ensureSize(this.virtualRegisters, virtReg.getId() + 1);
                this.virtualRegisters.set(virtReg.getId(), virtReg);
            });

            instruction.getReadRegisters().forEach(reg -> {
                var virtReg = (VirtualRegister) reg;
                kill.set(virtReg.getId(), false);
                gen.set(virtReg.getId(), true);

                ListUtils.ensureSize(this.virtualRegisters, virtReg.getId() + 1);
                this.virtualRegisters.set(virtReg.getId(), virtReg);
            });
        }

        return new GenKill(gen, kill);
    }

    private void calculateLifetimes() {
        // Generate Gen/Kill Sets for every basic block.
        var genKillSets = new HashMap<BasicBlock, GenKill>();
        for (var block : this.graph.getBlocks()) {
            genKillSets.put(block, this.calculateGenKillForBlock(block));
        }

        // Collect reverse control flow edges
        var reverseEdges = new HashMap<BasicBlock, List<BasicBlock>>();
        for (var block : this.graph.getBlocks()) {
            reverseEdges.putIfAbsent(block, new ArrayList<>());
            block.getLastInstruction().getTargets().forEach(targetBlock -> {
                reverseEdges.putIfAbsent(targetBlock, new ArrayList<>());
                reverseEdges.get(targetBlock).add(block);
            });
        }

        // Perform dataflow on blocks to find which are live at the end of a bb.
        var liveBeforeBlock = new HashMap<BasicBlock, BitSet>();
        this.graph.getBlocks().forEach(block -> liveBeforeBlock.put(block, new BitSet()));

        var clean = new HashSet<BasicBlock>();

        var queue = new ArrayDeque<>(this.graph.getBlocks());

        while (!queue.isEmpty()) {
            var block = queue.pollFirst();
            if (clean.contains(block)) continue;

            // Union of live variables of predecessor blocks.
            var liveAfterBlock = new BitSet();
            for (var predBlock : block.getLastInstruction().getTargets()) {
                liveAfterBlock.or(liveBeforeBlock.get(predBlock));
            }
            var genKill = genKillSets.get(block);

            // applying gen/kill schema
            liveAfterBlock.andNot(genKill.kill);
            liveAfterBlock.or(genKill.gen);

            // has the result changed?
            if (!liveAfterBlock.equals(liveBeforeBlock.get(block))) {
                clean.add(block);
                liveBeforeBlock.put(block, liveAfterBlock);

                for (var predBlock : reverseEdges.get(block)) {
                    queue.addLast(predBlock);
                    clean.remove(predBlock);
                }
            }
        }

        // Generated exact livetimes within a basic block
        for (var block : this.graph.getBlocks()) {
            this.liveIntervals.put(block, new HashMap<>());

            // Which registers are directly live after this block.
            var liveAfterBlock = new BitSet();
            for (var predBlock : block.getLastInstruction().getTargets()) {
                liveAfterBlock.or(liveBeforeBlock.get(predBlock));
            }

            var instructions = block.getInstructions();

            // store which virtual register is live and where it started in this block.
            var liveRegisters = new HashMap<VirtualRegister, Integer>();
            this.virtualRegisters.stream()
                    .filter(virtReg -> virtReg != null && liveAfterBlock.get(virtReg.getId()))
                    .forEach(virtReg -> liveRegisters.put(virtReg, instructions.size() - 1));

            for (int i = instructions.size() - 1; i >= 0; i--) {
                var instruction = instructions.get(i);

                int start = i;
                instruction.getWrittenRegister().ifPresent(reg -> {
                    var virtReg = (VirtualRegister) reg;

                    this.liveIntervals.get(block).computeIfAbsent(virtReg, ignored -> new SegmentInterval());
                    Integer stop = liveRegisters.remove(virtReg);

                    if (stop != null) {
                        this.liveIntervals.get(block).get(virtReg).addSegment(start, stop);
                    } else {
                        // Only a instruction with side effects might cause virtual registers that are never read
                        assert instruction instanceof CallInstruction
                                || instruction instanceof DivInstruction
                                || instruction instanceof MovInstruction mov && (mov.getSource() instanceof MemoryLocation || mov.getDestination() instanceof MemoryLocation);
                    }
                });

                int stop = i - 1;
                instruction.getReadRegisters().forEach(reg -> {
                    var virtReg = (VirtualRegister) reg;

                    if (!liveRegisters.containsKey(virtReg)) {
                        liveRegisters.put(virtReg, stop);
                    }
                });
            }

            // All remaining live registers in this block
            for (var pair : liveRegisters.entrySet()) {
                var liveReg = pair.getKey();
                var stop = pair.getValue();

                assert liveBeforeBlock.get(block).get(liveReg.getId());

                this.liveIntervals.get(block).computeIfAbsent(liveReg, ignored -> new SegmentInterval());
                this.liveIntervals.get(block).get(liveReg).addSegment(-1, stop); // -1 means that it is live before the first instruction.
            }
        }

        this.liveIntervals.values().forEach(m -> m.values().forEach(SegmentInterval::finish));
    }

    public static Lifetimes calculateLifetimes(SirGraph graph) {
        var lifetimes = new DataflowLifetimes(graph);

        lifetimes.calculateLifetimes();

        return new Lifetimes(lifetimes.liveIntervals);
    }
}
