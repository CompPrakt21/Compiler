package compiler.codegen;

public final class HardwareRegister extends Register {
    public static final HardwareRegister RAX = new HardwareRegister("rax");
    public static final HardwareRegister RBX = new HardwareRegister("rbx");
    public static final HardwareRegister RCX = new HardwareRegister("rcx");
    public static final HardwareRegister RDX = new HardwareRegister("rdx");
    public static final HardwareRegister RSI = new HardwareRegister("rsi");
    public static final HardwareRegister RDI = new HardwareRegister("rdi");

    public static final HardwareRegister R8 = new HardwareRegister("r8");
    public static final HardwareRegister R9 = new HardwareRegister("r9");
    public static final HardwareRegister R10 = new HardwareRegister("r10");
    public static final HardwareRegister R11 = new HardwareRegister("r11");
    public static final HardwareRegister R12 = new HardwareRegister("r12");
    public static final HardwareRegister R13 = new HardwareRegister("r13");
    public static final HardwareRegister R14 = new HardwareRegister("r14");
    public static final HardwareRegister R15 = new HardwareRegister("r15");

    private final String name;

    private HardwareRegister(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
