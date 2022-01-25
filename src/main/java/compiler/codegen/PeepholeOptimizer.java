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

        // add r1 c1
        // add r1 c2
        // This pattern occurs when there is call, immediately before the current functions end.
        // The stackspace cleanup of the called function arguments is the first add.
        // The stackspace cleanup of the current functions local variables is the second add.
        } else if (instructions.get(index) instanceof AddInstruction add1
                && instructions.size() > index + 1
                && instructions.get(index + 1) instanceof AddInstruction add2
                && add1.getTarget().equals(add2.getTarget())
                && add1.getLhs().equals(add2.getLhs())
                && add1.getRhs() instanceof Constant c1
                && add2.getRhs() instanceof Constant c2
                && Util.fitsInto32Bit(c1.getValue() + c2.getValue())
        ) {
            assert add1.getLhs().equals(add2.getLhs());

            var replacement = new AddInstruction(add1.getTarget(), add1.getLhs(), new Constant(c1.getValue() + c2.getValue()));
            return Optional.of(new Substitution(2, List.of(replacement)));
        }

        return Optional.empty();
    }
}
