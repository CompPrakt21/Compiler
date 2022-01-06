package compiler.codegen;

import java.util.HashMap;
import java.util.Map;

public class StackSlots {
    private int currentOffset;

    private Map<VirtualRegister, Integer> offsets;

    public StackSlots() {
        this.currentOffset = 0;
        this.offsets = new HashMap<>();
    }

    private int allocateNewSpace(Register.Width width) {
        var bytes = width.getByteSize();

        var paddingNeeded = Math.floorMod(currentOffset, bytes);
        if (paddingNeeded != 0) {
            this.currentOffset -= paddingNeeded;

            assert Math.floorMod(currentOffset, bytes) == 0;
        }

        this.currentOffset -= bytes;

        return currentOffset;
    }

    public int get(VirtualRegister register) {
        Integer offset = this.offsets.get(register);

        if (offset == null) {
            var newOffset = this.allocateNewSpace(register.getWidth());
            this.offsets.put(register, newOffset);
            return newOffset;
        }

        return offset;
    }

    public int getNeededStackSpace() {
        return this.currentOffset;
    }

    /**
     * Explicitely map a virtual register to an offset.
     * This is used for function parameters which are a placed at a negativ offset
     * compared to rbp.
     */
    public void mapRegister(VirtualRegister register, int offset) {
        assert offset >= 0;
        this.offsets.put(register, offset);
    }
}
