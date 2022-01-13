package compiler.codegen.sir.instructions;

import compiler.codegen.*;
import compiler.codegen.Register.Width;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class MovInstruction extends Instruction {
    private Register.Width width;
    private Operand destination;
    private Operand source;

    public MovInstruction(Register.Width width, Operand destination, Operand source) {

        assert !(destination instanceof Register dest) || !(source instanceof Register src) || dest != src;

        this.width = width;
        this.destination = destination;
        this.source = source;

        assert this.verify();
    }

    private boolean verify() {
        //assert !(destination instanceof HardwareRegister dest) || !(source instanceof HardwareRegister src) || dest != src;
        return (this.destination instanceof Register || this.source instanceof Register)
                && !(this.destination instanceof Constant);
    }

    public Register.Width getWidth() {
        return width;
    }

    public void setWidth(Register.Width width) {
        this.width = width;
    }

    public Operand getDestination() {
        return destination;
    }

    public void setDestination(Operand destination) {
        this.destination = destination;
        assert this.verify();
    }

    public Operand getSource() {
        return source;
    }

    public void setSource(Operand source) {
        this.source = source;
        assert this.verify();
    }

    @Override
    public String getMnemonic() {
        return "mov";
    }

    @Override
    public List<Register> getReadRegisters() {
        if (this.destination instanceof MemoryLocation loc) {
            return Stream.concat(this.source.getRegisters().stream(), loc.getRegisters().stream()).toList();
        } else {
            return this.source.getRegisters();
        }
    }

    @Override
    public Optional<Register> getWrittenRegister() {
        if (this.destination instanceof Register reg) {
            return Optional.of(reg);
        } else {
            return Optional.empty();
        }
    }
}
