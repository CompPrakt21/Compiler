package compiler.codegen;

import compiler.codegen.llir.*;

public class AssemblyEmitter extends Emitter {

    public AssemblyEmitter() {
        super();
        this.append(".text\n\n");
    }

    @Override
    public void beginFunction(String name, int numArgs, boolean isVoid) {
        this.append(String.format(".globl\t%s", name));
        this.append(String.format(".type\t%s, @function", name));
        this.append(name +  ":");
    }

    @Override
    public void endFunction() {}

    @Override
    public void beginBlock(BasicBlock block) {
        this.append(block.getLabel() + ":");
    }

    @Override
    public void endBlock(ControlFlowNode endNode) {
        // Do nothing for now, might be necessary elsewhere
    }

    @Override
    public void emitInstruction(LlirNode node) {
        // TODO
    }
}
