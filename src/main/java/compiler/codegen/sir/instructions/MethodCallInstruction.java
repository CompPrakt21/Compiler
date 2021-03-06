package compiler.codegen.sir.instructions;

import compiler.codegen.Register;
import compiler.semantic.resolution.MethodDefinition;

import java.util.ArrayList;
import java.util.List;

public final class MethodCallInstruction extends CallInstruction {
    private MethodDefinition method;
    private List<Register> arguments;

    public MethodCallInstruction(Register target, MethodDefinition method, List<Register> arguments) {
        super(target);
        this.method = method;
        this.arguments = new ArrayList<>(arguments);
    }

    public List<Register> getArguments() {
        return arguments;
    }

    public MethodDefinition getMethod() {
        return method;
    }

    @Override
    public String getMnemonic() {
        return "call";
    }

    @Override
    public List<Register> getReadRegisters() {
        return this.arguments;
    }
}
