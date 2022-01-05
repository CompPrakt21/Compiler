package compiler.codegen.sir.instructions;

import compiler.codegen.Register;

public final class AllocCallInstruction extends CallInstruction {

    private Register objectSize;
    private Register numElements;

    public AllocCallInstruction(Register target, Register objectSize, Register numElements) {
        super(target);
        this.objectSize = objectSize;
        this.numElements = numElements;
    }

    public Register getObjectSize() {
        return objectSize;
    }

    public void setObjectSize(Register objectSize) {
        this.objectSize = objectSize;
    }

    public Register getNumElements() {
        return numElements;
    }

    public void setNumElements(Register numElements) {
        this.numElements = numElements;
    }

    @Override
    public String getMnemonic() {
        return "call <alloc>";
    }
}
