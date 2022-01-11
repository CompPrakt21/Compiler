package compiler.codegen;

import java.util.Objects;

public final class VirtualRegister extends Register {
    private final int id;
    private final Width width;

    private VirtualRegister(int id, Width width) {
        this.id = id;
        this.width = width;
    }

    public int getId() {
        return this.id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualRegister that = (VirtualRegister) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String getName() {
        var width = switch (this.getWidth()) {
            case BIT8 -> "v(8)";
            case BIT32 -> "v";
            case BIT64 -> "V";
        };
        return String.format("%s%s", width, getId());
    }

    @Override
    public Width getWidth() {
        return this.width;
    }

    public static class Generator {
        private int nextId;

        public Generator() {
            this.nextId = 1;
        }

        public VirtualRegister nextRegister(Width width) {
            var reg = new VirtualRegister(this.nextId, width);
            this.nextId += 1;
            return reg;
        }
    }
}
