package compiler.codegen;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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

    private boolean verify() {
        var scaleIsPowerOfTwo = scale != 0 && ((scale & (scale - 1)) == 0);
        var eitherBaseOrConstant =  this.baseRegister.isPresent() || constant != 0;
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
}
