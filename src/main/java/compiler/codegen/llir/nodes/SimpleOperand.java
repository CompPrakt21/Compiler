package compiler.codegen.llir.nodes;

import java.util.List;

public sealed interface SimpleOperand extends Operand permits Constant, RegisterNode {
}
