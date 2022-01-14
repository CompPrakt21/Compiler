package compiler.codegen;

import compiler.codegen.sir.SirGraph;
import compiler.codegen.sir.instructions.*;
import firm.nodes.Const;
import firm.nodes.Sub;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeepholeOptimizer {
    private final SirGraph graph;

    public PeepholeOptimizer(SirGraph graph) {
        this.graph = graph;
    }

    public void optimize() {
        for (var bb : this.graph.getBlocks()) {
            List<Instruction> newList = new ArrayList<>();
            var currentInstructions = bb.getInstructions();

            for (int i = 0; i < currentInstructions.size();) {
                var substitution = this.optimizeAt(i, currentInstructions);

                if (substitution.isEmpty()) {
                    newList.add(currentInstructions.get(i));
                } else {
                    assert substitution.get().removedInstruction > 0;
                    newList.addAll(substitution.get().replacement);
                }

                i += substitution.map(value -> value.removedInstruction).orElse(1);
            }

            bb.setInstructions(newList);
        }
    }

    private record Substitution(int removedInstruction, List<Instruction> replacement){}

    private Optional<Substitution> optimizeAt(int index, List<Instruction> instructions) {
        // mov rX rX
        if (instructions.get(index) instanceof MovInstruction mov
                && mov.getSource() instanceof HardwareRegister source
                && mov.getDestination() instanceof HardwareRegister dest
                && source == dest
        ) {
                return Optional.of(new Substitution(1, List.of()));

        // add/sub r/mX 0
        } else if (instructions.get(index) instanceof BinaryInstruction instr
                && ((instr instanceof AddInstruction) || (instr instanceof SubInstruction))
                && instr.getRhs() instanceof Constant c
                && c.getValue() == 0
        ) {
            return Optional.of(new Substitution(1, List.of()));
        }

        return Optional.empty();
    }
}
