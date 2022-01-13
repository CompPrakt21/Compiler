package compiler.codegen;

import compiler.utils.BiMap;

import java.util.*;

public class RegisterManager {
    private final ArrayDeque<HardwareRegister.Group> freeRegisters;

    private final BiMap<VirtualRegister, HardwareRegister.Group> virtualRegLocation;

    public RegisterManager() {
        this.freeRegisters = new ArrayDeque<>();
        this.virtualRegLocation = new BiMap<>();

        this.addAllRegisters();
    }

    private boolean freeRegistersUnique() {
        var set = EnumSet.noneOf(HardwareRegister.Group.class);

        for (var freeReg : this.freeRegisters) {
            if (set.contains(freeReg)) {
                return false;
            } else {
                set.add(freeReg);
            }
        }

        return true;
    }

    private void addAllRegisters() {
        this.freeRegisters.addFirst(HardwareRegister.Group.R8 );
        this.freeRegisters.addFirst(HardwareRegister.Group.R9 );
        this.freeRegisters.addFirst(HardwareRegister.Group.R10);
        this.freeRegisters.addFirst(HardwareRegister.Group.R11);
        this.freeRegisters.addFirst(HardwareRegister.Group.R12);
        this.freeRegisters.addFirst(HardwareRegister.Group.R13);
        this.freeRegisters.addFirst(HardwareRegister.Group.R14);
        this.freeRegisters.addFirst(HardwareRegister.Group.R15);
        this.freeRegisters.addFirst(HardwareRegister.Group.C);
        this.freeRegisters.addFirst(HardwareRegister.Group.B);

        // These registers are sometimes specifically needed.
        // In order to reduce the chance that they are not availabe in these
        // situations we put them last in the queue so that the other registers
        // are used up first.
        this.freeRegisters.addLast(HardwareRegister.Group.SI);
        this.freeRegisters.addLast(HardwareRegister.Group.DI);
        this.freeRegisters.addLast(HardwareRegister.Group.D);
        this.freeRegisters.addLast(HardwareRegister.Group.A);
    }

    public Optional<HardwareRegister> getOrCreateMapping(VirtualRegister virtReg) {
        var alreadyMapped = this.getMapping(virtReg);

        if (alreadyMapped.isPresent()) {
            return alreadyMapped;
        } else {
            return this.createMapping(virtReg);
        }
    }

    public Optional<HardwareRegister> getMapping(VirtualRegister virtReg) {
        return Optional.ofNullable(this.virtualRegLocation.getRight(virtReg)).map(group -> group.getRegister(virtReg.getWidth()));
    }

    public Optional<HardwareRegister> createMapping(VirtualRegister virtReg) {
        Optional<HardwareRegister.Group> hardwareReg;

        if (!this.freeRegisters.isEmpty()) {
            hardwareReg = Optional.of(this.freeRegisters.removeFirst());
        } else {
            hardwareReg = Optional.empty();
        }

        hardwareReg.ifPresent(reg -> this.virtualRegLocation.put(virtReg, reg));

        return hardwareReg.map(group -> group.getRegister(virtReg.getWidth()));
    }

    public Optional<HardwareRegister> createSpecificMapping(VirtualRegister virtReg, HardwareRegister hardwareReg) {
        //assert this.getMapping(virtReg).isEmpty();

        var result = this.getHardwareRegister(hardwareReg);
        if (result.isPresent()) {
            this.virtualRegLocation.put(virtReg, hardwareReg.getGroup());
        }

        return result;
    }

    public void freeMapping(VirtualRegister register) {
        assert this.getMapping(register).isPresent();

        var reg = this.virtualRegLocation.removeLeft(register).orElseThrow();

        this.freeHardwareRegister(reg.getRegister(register.getWidth()));
    }

    public Optional<HardwareRegister> getOrCreateSpecificMapping(VirtualRegister virtReg, HardwareRegister hardwareReg) {
        assert virtReg.getWidth() == hardwareReg.getWidth();

        var alreadyMapped = this.virtualRegLocation.getRight(virtReg);
        if (alreadyMapped != null && alreadyMapped == hardwareReg.getGroup()) {
            return Optional.of(alreadyMapped.getRegister(virtReg.getWidth()));
        }

        return this.createSpecificMapping(virtReg, hardwareReg);
    }

    public Optional<HardwareRegister> getHardwareRegister(HardwareRegister reg) {
        var wasRemoved = this.freeRegisters.remove(reg.getGroup());

        if (wasRemoved) {
            return Optional.of(reg);
        } else {
            return Optional.empty();
        }
    }

    public void clearAllMappings() {
        this.virtualRegLocation.clear();
        this.freeRegisters.clear();
        this.addAllRegisters();
    }

    public void freeHardwareRegister(HardwareRegister reg) {
        assert !this.freeRegisters.contains(reg.getGroup());

        var group = reg.getGroup();

        if (group.equals(HardwareRegister.Group.A) || group.equals(HardwareRegister.Group.D) || group.equals(HardwareRegister.Group.SI) || group.equals(HardwareRegister.Group.DI)) {
            this.freeRegisters.addLast(group);
        } else {
            this.freeRegisters.addFirst(group);
        }

        assert this.freeRegistersUnique();
    }

    public BiMap<VirtualRegister, HardwareRegister.Group> getMapping() {
        return this.virtualRegLocation;
    }

    public boolean isAvailable(HardwareRegister.Group reg) {
        return this.freeRegisters.contains(reg);
    }

    public VirtualRegister getMappedVirtualRegister(HardwareRegister.Group group) {
        return this.virtualRegLocation.getLeft(group);
    }

    public Collection<HardwareRegister.Group> availableRegisters() {
        return this.freeRegisters;
    }
}
