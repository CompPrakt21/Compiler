package compiler.codegen;

import java.util.Objects;

public final class VirtualRegister extends Register {
    private final int id;

    private VirtualRegister(int id) {
        this.id = id;
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

    public static class Generator {
        private int nextId;

        public Generator() {
            this.nextId = 1;
        }

        public VirtualRegister nextRegister() {
            var reg = new VirtualRegister(this.nextId);
            this.nextId += 1;
            return reg;
        }
    }
}
