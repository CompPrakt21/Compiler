package compiler.codegen.llir.nodes;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public final class MemoryLocation implements Operand {
    private Optional<RegisterNode> baseRegister;
    private int constant;
    private Optional<RegisterNode> index;
    private int scale;

    public MemoryLocation() {
        this.baseRegister = Optional.empty();
        this.constant = 0;
        this.index = Optional.empty();
        this.scale = 1;
    }

    public static MemoryLocation base(RegisterNode baseRegister) {
        var loc = new MemoryLocation();
        loc.baseRegister = Optional.of(baseRegister);
        assert loc.verify();
        return loc;
    }

    public static MemoryLocation constant(int constant) {
        var loc = new MemoryLocation();
        loc.constant = constant;
        assert loc.verify();
        return loc;
    }

    public static MemoryLocation baseConstant(RegisterNode baseRegister, int constant) {
        var loc = new MemoryLocation();
        loc.baseRegister = Optional.of(baseRegister);
        loc.constant = constant;
        assert loc.verify();
        return loc;
    }

    public static MemoryLocation baseIndex(RegisterNode baseRegister, RegisterNode indexRegister) {
        var loc = new MemoryLocation();
        loc.baseRegister = Optional.of(baseRegister);
        loc.index = Optional.of(indexRegister);
        assert loc.verify();
        return loc;
    }

    public static MemoryLocation indexScale(RegisterNode indexRegister, int scale) {
        var loc = new MemoryLocation();
        loc.index = Optional.of(indexRegister);
        loc.scale = scale;
        assert loc.verify();
        return loc;
    }

    public static MemoryLocation baseIndexScale(RegisterNode baseRegister, RegisterNode indexRegister, int scale) {
        var loc = new MemoryLocation();
        loc.baseRegister = Optional.of(baseRegister);
        loc.index = Optional.of(indexRegister);
        loc.scale = scale;
        assert loc.verify();
        return loc;
    }

    public boolean verify() {
        var scaleIsPowerOfTwo = scale != 0 && ((scale & (scale - 1)) == 0);
        var scaleRequiresIndex = scale == 1 || index.isPresent();

        return scaleIsPowerOfTwo && scaleRequiresIndex;
    }

    public void setBaseRegister(RegisterNode baseRegister) {
        this.baseRegister = Optional.of(baseRegister);
        assert this.verify();
    }

    public void setConstant(int constant) {
        this.constant = constant;
        assert this.verify();
    }

    public void setIndex(RegisterNode index) {
        this.index = Optional.of(index);
        assert this.verify();
    }

    public void setScale(int scale) {
        this.scale = scale;
        assert this.verify();
    }

    public Optional<RegisterNode> getBaseRegister() {
        return baseRegister;
    }

    public int getConstant() {
        return constant;
    }

    public Optional<RegisterNode> getIndex() {
        return index;
    }

    public int getScale() {
        return scale;
    }

    public String formatIntelSyntax() {
        StringBuilder s = new StringBuilder("[");

        boolean nonEmpty = false;

        if (this.baseRegister.isPresent()) {
            s.append(this.baseRegister.get().getTargetRegister());
            nonEmpty = true;
        }

        if (this.constant != 0) {
            if (nonEmpty) s.append(" + ");
            s.append(this.constant);
            nonEmpty = true;
        }

        if (this.index.isPresent()) {
            if (nonEmpty) s.append(" + ");
            s.append(this.index.get().getTargetRegister());
        }

        if (this.scale > 1) {
            s.append(" * ");
            s.append(this.scale);
        }

        s.append("]");
        return s.toString();
    }

    @Override
    public List<RegisterNode> getRegisters() {
        return Stream.concat(this.baseRegister.stream(), this.index.stream()).toList();
    }
}
