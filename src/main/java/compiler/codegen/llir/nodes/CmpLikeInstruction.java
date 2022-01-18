package compiler.codegen.llir.nodes;

import compiler.codegen.Predicate;

public sealed interface CmpLikeInstruction permits CmpInstruction, CmpFromMemInstruction {
    /**
     * @return if the arguments of the compare instruction were reversed.
     * (This might happen during instruction selection)
     */
    boolean hasReversedArguments();
}
