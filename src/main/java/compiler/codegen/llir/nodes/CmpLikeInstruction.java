package compiler.codegen.llir.nodes;

import compiler.codegen.Predicate;

public sealed interface CmpLikeInstruction permits CmpInstruction, CmpFromMemInstruction {
}
