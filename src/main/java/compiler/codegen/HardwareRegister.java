package compiler.codegen;

public final class HardwareRegister extends Register {
    public static final HardwareRegister RAX = new HardwareRegister("rax", Width.BIT64);
    public static final HardwareRegister RBX = new HardwareRegister("rbx", Width.BIT64);
    public static final HardwareRegister RCX = new HardwareRegister("rcx", Width.BIT64);
    public static final HardwareRegister RDX = new HardwareRegister("rdx", Width.BIT64);
    public static final HardwareRegister RSI = new HardwareRegister("rsi", Width.BIT64);
    public static final HardwareRegister RDI = new HardwareRegister("rdi", Width.BIT64);

    public static final HardwareRegister EAX = new HardwareRegister("eax", Width.BIT32);
    public static final HardwareRegister EBX = new HardwareRegister("ebx", Width.BIT32);
    public static final HardwareRegister ECX = new HardwareRegister("ecx", Width.BIT32);
    public static final HardwareRegister EDX = new HardwareRegister("edx", Width.BIT32);
    public static final HardwareRegister ESI = new HardwareRegister("esi", Width.BIT32);
    public static final HardwareRegister EDI = new HardwareRegister("edi", Width.BIT32);

    public static final HardwareRegister R8 = new HardwareRegister("r8", Width.BIT64);
    public static final HardwareRegister R9 = new HardwareRegister("r9", Width.BIT64);
    public static final HardwareRegister R10 = new HardwareRegister("r10", Width.BIT64);
    public static final HardwareRegister R11 = new HardwareRegister("r11", Width.BIT64);
    public static final HardwareRegister R12 = new HardwareRegister("r12", Width.BIT64);
    public static final HardwareRegister R13 = new HardwareRegister("r13", Width.BIT64);
    public static final HardwareRegister R14 = new HardwareRegister("r14", Width.BIT64);
    public static final HardwareRegister R15 = new HardwareRegister("r15", Width.BIT64);

    public static final HardwareRegister R8D = new HardwareRegister("r8d", Width.BIT32);
    public static final HardwareRegister R9D = new HardwareRegister("r9d", Width.BIT32);
    public static final HardwareRegister R10D = new HardwareRegister("r10d", Width.BIT32);
    public static final HardwareRegister R11D = new HardwareRegister("r11d", Width.BIT32);
    public static final HardwareRegister R12D = new HardwareRegister("r12d", Width.BIT32);
    public static final HardwareRegister R13D = new HardwareRegister("r13d", Width.BIT32);
    public static final HardwareRegister R14D = new HardwareRegister("r14d", Width.BIT32);
    public static final HardwareRegister R15D = new HardwareRegister("r15d", Width.BIT32);

    public static final HardwareRegister RSP = new HardwareRegister("rsp", Width.BIT64);
    public static final HardwareRegister RBP = new HardwareRegister("rbp", Width.BIT64);

    private final String name;
    private final Width width;

    private HardwareRegister(String name, Width width) {
        this.name = name;
        this.width = width;
    }

    public HardwareRegister forWidth(Register.Width width) {
        if (this.width == width) {
            return this;
        }

        if (width == Width.BIT64) {
            return switch (this.name) {
                case "eax" -> HardwareRegister.RAX;
                case "ebx" -> HardwareRegister.RBX;
                case "ecx" -> HardwareRegister.RCX;
                case "edx" -> HardwareRegister.RDX;
                case "esi" -> HardwareRegister.RSI;
                case "edi" -> HardwareRegister.RDI;
                case "r8d"  -> HardwareRegister.R8;
                case "r9d"  -> HardwareRegister.R9;
                case "r10d" -> HardwareRegister.R10;
                case "r11d" -> HardwareRegister.R11;
                case "r12d" -> HardwareRegister.R12;
                case "r13d" -> HardwareRegister.R13;
                case "r14d" -> HardwareRegister.R14;
                case "r15d" -> HardwareRegister.R15;
                default -> throw new IllegalArgumentException();
            };
        } else {
            return switch (this.name) {
                case "rax" -> HardwareRegister.EAX;
                case "rbx" -> HardwareRegister.EBX;
                case "rcx" -> HardwareRegister.ECX;
                case "rdx" -> HardwareRegister.EDX;
                case "rsi" -> HardwareRegister.ESI;
                case "rdi" -> HardwareRegister.EDI;
                case "r8"  -> HardwareRegister.R8D;
                case "r9"  -> HardwareRegister.R9D;
                case "r10" -> HardwareRegister.R10D;
                case "r11" -> HardwareRegister.R11D;
                case "r12" -> HardwareRegister.R12D;
                case "r13" -> HardwareRegister.R13D;
                case "r14" -> HardwareRegister.R14D;
                case "r15" -> HardwareRegister.R15D;
                default -> throw new IllegalArgumentException();
            };
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Width getWidth() {
        return this.width;
    }
}
