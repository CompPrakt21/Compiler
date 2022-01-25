package compiler.codegen;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public final class MemoryLocation extends Operand {
    private Optional<Register> baseRegister;
    private int constant;
    private Optional<Register> index;
    private int scale;

    public MemoryLocation() {
        this.baseRegister = Optional.empty();
        this.constant = 0;
        this.index = Optional.empty();
        this.scale = 1;
    }

    public MemoryLocation(Register baseRegister) {
        this();
        this.baseRegister = Optional.of(baseRegister);

        assert verify();
    }

    public MemoryLocation(Register baseRegister, int constant) {
        this();
        this.baseRegister = Optional.of(baseRegister);
        this.constant = constant;

        assert verify();
    }

    public MemoryLocation(Optional<Register> baseRegister, int constant, Optional<Register> index, int scale) {
        this();
        this.baseRegister = baseRegister;
        this.constant = constant;
        this.index = index;
        this.scale = scale;

        assert verify();
    }

    private boolean verify() {
        var scaleIsPowerOfTwo = scale != 0 && ((scale & (scale - 1)) == 0);
        var eitherBaseOrConstant =  this.baseRegister.isPresent() || constant != 0 || this.index.isPresent();
        var scaleRequiresIndex = scale == 1 || index.isPresent();

        return scaleIsPowerOfTwo && eitherBaseOrConstant && scaleRequiresIndex;
    }

    public Optional<Register> getBaseRegister() {
        return baseRegister;
    }

    public int getConstant() {
        return constant;
    }

    public Optional<Register> getIndex() {
        return index;
    }

    public int getScale() {
        return scale;
    }

    public void setBaseRegister(Register baseRegister) {
        this.baseRegister = Optional.of(baseRegister);
    }

    public void setConstant(int constant) {
        this.constant = constant;
    }

    public void setIndex(Register index) {
        this.index = Optional.of(index);
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public String formatIntelSyntax() {
        StringBuilder s = new StringBuilder("[");

        boolean nonEmpty = false;

        if (this.baseRegister.isPresent()) {
            s.append(this.baseRegister.get());
            nonEmpty = true;
        }

        if (this.constant != 0) {
            if (nonEmpty) s.append(" + ");
            s.append(this.constant);
            nonEmpty = true;
        }

        if (this.index.isPresent()) {
            if (nonEmpty) s.append(" + ");
            s.append(this.index.get());
        }

        if (this.scale > 1) {
            s.append(" * ");
            s.append(this.scale);
        }

        s.append("]");
        return s.toString();
    }

    public String formatATTSyntax() {
        StringBuilder s = new StringBuilder();

        if (this.constant != 0) {
            s.append(this.constant);
        }

        s.append("(");

        if (this.baseRegister.isPresent()) {
            s.append("%");
            s.append(this.baseRegister.get());
        }

        if (this.index.isPresent()) {
            s.append(",%");
            s.append(this.index.get());
        }

        if (this.scale > 1) {
            s.append(",");
            s.append(this.scale);
        }

        s.append(")");
        return s.toString();
    }

    @Override
    public List<Register> getRegisters() {
        return Stream.concat(this.baseRegister.stream(), this.index.stream()).toList();
    }
}
