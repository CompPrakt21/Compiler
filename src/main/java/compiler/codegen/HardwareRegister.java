package compiler.codegen;

public final class HardwareRegister extends Register {
    public static final HardwareRegister RAX = new HardwareRegister(1, "rax", Width.BIT64);
    public static final HardwareRegister RBX = new HardwareRegister(2, "rbx", Width.BIT64);
    public static final HardwareRegister RCX = new HardwareRegister(3, "rcx", Width.BIT64);
    public static final HardwareRegister RDX = new HardwareRegister(4, "rdx", Width.BIT64);
    public static final HardwareRegister RSI = new HardwareRegister(5, "rsi", Width.BIT64);
    public static final HardwareRegister RDI = new HardwareRegister(6, "rdi", Width.BIT64);

    public static final HardwareRegister EAX = new HardwareRegister(1, "eax", Width.BIT32);
    public static final HardwareRegister EBX = new HardwareRegister(2, "ebx", Width.BIT32);
    public static final HardwareRegister ECX = new HardwareRegister(3, "ecx", Width.BIT32);
    public static final HardwareRegister EDX = new HardwareRegister(4, "edx", Width.BIT32);
    public static final HardwareRegister ESI = new HardwareRegister(5, "esi", Width.BIT32);
    public static final HardwareRegister EDI = new HardwareRegister(6, "edi", Width.BIT32);

    public static final HardwareRegister AL = new HardwareRegister(1, "al", Width.BIT8);
    public static final HardwareRegister BL = new HardwareRegister(2, "bl", Width.BIT8);
    public static final HardwareRegister CL = new HardwareRegister(3, "cl", Width.BIT8);
    public static final HardwareRegister DL = new HardwareRegister(4, "dl", Width.BIT8);
    public static final HardwareRegister SIL = new HardwareRegister(5, "sil", Width.BIT8);
    public static final HardwareRegister DIL = new HardwareRegister(6, "dil", Width.BIT8);

    public static final HardwareRegister R8 = new HardwareRegister(7, "r8", Width.BIT64);
    public static final HardwareRegister R9 = new HardwareRegister(8, "r9", Width.BIT64);
    public static final HardwareRegister R10 = new HardwareRegister(9,"r10", Width.BIT64);
    public static final HardwareRegister R11 = new HardwareRegister(10,"r11", Width.BIT64);
    public static final HardwareRegister R12 = new HardwareRegister(11,"r12", Width.BIT64);
    public static final HardwareRegister R13 = new HardwareRegister(12,"r13", Width.BIT64);
    public static final HardwareRegister R14 = new HardwareRegister(13,"r14", Width.BIT64);
    public static final HardwareRegister R15 = new HardwareRegister(14,"r15", Width.BIT64);

    public static final HardwareRegister R8D = new HardwareRegister(7, "r8d", Width.BIT32);
    public static final HardwareRegister R9D = new HardwareRegister(8, "r9d", Width.BIT32);
    public static final HardwareRegister R10D = new HardwareRegister(9, "r10d", Width.BIT32);
    public static final HardwareRegister R11D = new HardwareRegister(10, "r11d", Width.BIT32);
    public static final HardwareRegister R12D = new HardwareRegister(11, "r12d", Width.BIT32);
    public static final HardwareRegister R13D = new HardwareRegister(12, "r13d", Width.BIT32);
    public static final HardwareRegister R14D = new HardwareRegister(13, "r14d", Width.BIT32);
    public static final HardwareRegister R15D = new HardwareRegister(14, "r15d", Width.BIT32);

    public static final HardwareRegister R8B = new HardwareRegister(7, "r8b", Width.BIT8);
    public static final HardwareRegister R9B = new HardwareRegister(8, "r9b", Width.BIT8);
    public static final HardwareRegister R10B = new HardwareRegister(9, "r10b", Width.BIT8);
    public static final HardwareRegister R11B = new HardwareRegister(10, "r11b", Width.BIT8);
    public static final HardwareRegister R12B = new HardwareRegister(11, "r12b", Width.BIT8);
    public static final HardwareRegister R13B = new HardwareRegister(12, "r13b", Width.BIT8);
    public static final HardwareRegister R14B = new HardwareRegister(13, "r14b", Width.BIT8);
    public static final HardwareRegister R15B = new HardwareRegister(14, "r15b", Width.BIT8);

    public static final HardwareRegister RSP = new HardwareRegister(100, "rsp", Width.BIT64);
    public static final HardwareRegister RBP = new HardwareRegister(101, "rbp", Width.BIT64);

    private final String name;
    private final Width width;

    // A hardware register with each of its different widths forms a group. (eg. rax, eax and al)
    private final int group;

    private HardwareRegister(int group, String name, Width width) {
        this.name = name;
        this.width = width;
        this.group = group;
    }

    public HardwareRegister forWidth(Register.Width width) {
        if (this.width == width) {
            return this;
        }

        if (width == Width.BIT64) {
            return switch (this.group) {
                case 1 -> HardwareRegister.RAX;
                case 2 -> HardwareRegister.RBX;
                case 3 -> HardwareRegister.RCX;
                case 4 -> HardwareRegister.RDX;
                case 5 -> HardwareRegister.RSI;
                case 6 -> HardwareRegister.RDI;
                case 7 -> HardwareRegister.R8;
                case 8 -> HardwareRegister.R9;
                case 9 -> HardwareRegister.R10;
                case 10 -> HardwareRegister.R11;
                case 11 -> HardwareRegister.R12;
                case 12 -> HardwareRegister.R13;
                case 13 -> HardwareRegister.R14;
                case 14 -> HardwareRegister.R15;
                default -> throw new IllegalArgumentException();
            };
        } else if (width == Width.BIT32){
            return switch (this.group) {
                case 1 -> HardwareRegister.EAX;
                case 2 -> HardwareRegister.EBX;
                case 3 -> HardwareRegister.ECX;
                case 4 -> HardwareRegister.EDX;
                case 5 -> HardwareRegister.ESI;
                case 6 -> HardwareRegister.EDI;
                case 7 -> HardwareRegister.R8D;
                case 8 -> HardwareRegister.R9D;
                case 9 -> HardwareRegister.R10D;
                case 10 -> HardwareRegister.R11D;
                case 11 -> HardwareRegister.R12D;
                case 12 -> HardwareRegister.R13D;
                case 13 -> HardwareRegister.R14D;
                case 14 -> HardwareRegister.R15D;
                default -> throw new IllegalArgumentException();
            };
        } else if (width == Width.BIT8){
            return switch (this.group) {
                case 1 -> HardwareRegister.AL;
                case 2 -> HardwareRegister.BL;
                case 3 -> HardwareRegister.CL;
                case 4 -> HardwareRegister.DL;
                case 5 -> HardwareRegister.SIL;
                case 6 -> HardwareRegister.DIL;
                case 7 -> HardwareRegister.R8B;
                case 8 -> HardwareRegister.R9B;
                case 9 -> HardwareRegister.R10B;
                case 10 -> HardwareRegister.R11B;
                case 11 -> HardwareRegister.R12B;
                case 12 -> HardwareRegister.R13B;
                case 13 -> HardwareRegister.R14B;
                case 14 -> HardwareRegister.R15B;
                default -> throw new IllegalArgumentException();
            };
        } else {
            throw new IllegalArgumentException();
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
