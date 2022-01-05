package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public abstract sealed class CallInstruction extends RegisterInstruction permits AllocCallInstruction, MethodCallInstruction {
    public CallInstruction(Register target) {
        super(target);
    }
}
