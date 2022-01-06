package compiler.codegen.sir.instructions;

public final class LeaveInstruction extends Instruction {
    @Override
    public String getMnemonic() {
        return "leave";
    }
}
