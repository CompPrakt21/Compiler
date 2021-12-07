package compiler.codegen;

import java.util.List;

public class ReturnInstruction extends ControlFlowNode {

    private RegisterNode returnValue;

    public ReturnInstruction(RegisterNode returnValue) {
        super(List.of());
        this.returnValue = returnValue;
    }

    public RegisterNode getReturnValue() {
        return this.returnValue;
    }

    @Override
    public String getMnemonic() {
        return "ret";
    }
}
