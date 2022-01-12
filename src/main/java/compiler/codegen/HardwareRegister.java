package compiler.codegen;

public final class HardwareRegister extends Register {

    public enum Group {
        A, B, C, D, SI, DI, R8, R9, R10, R11, R12, R13, R14, R15, RBP, RSP;

        public HardwareRegister getRegister(Register.Width width) {
            if (width == Width.BIT64) {
                return switch (this) {
                    case A -> HardwareRegister.RAX;
                    case B -> HardwareRegister.RBX;
                    case C -> HardwareRegister.RCX;
                    case D -> HardwareRegister.RDX;
                    case SI -> HardwareRegister.RSI;
                    case DI -> HardwareRegister.RDI;
                    case R8 -> HardwareRegister.R8;
                    case R9 -> HardwareRegister.R9;
                    case R10 -> HardwareRegister.R10;
                    case R11 -> HardwareRegister.R11;
                    case R12 -> HardwareRegister.R12;
                    case R13 -> HardwareRegister.R13;
                    case R14 -> HardwareRegister.R14;
                    case R15 -> HardwareRegister.R15;
                    case RBP -> HardwareRegister.RBP;
                    case RSP -> HardwareRegister.RSP;
                };
            } else if (width == Width.BIT32){
                return switch (this) {
                    case A -> HardwareRegister.EAX;
                    case B -> HardwareRegister.EBX;
                    case C -> HardwareRegister.ECX;
                    case D -> HardwareRegister.EDX;
                    case SI -> HardwareRegister.ESI;
                    case DI -> HardwareRegister.EDI;
                    case R8 -> HardwareRegister.R8D;
                    case R9 -> HardwareRegister.R9D;
                    case R10 -> HardwareRegister.R10D;
                    case R11 -> HardwareRegister.R11D;
                    case R12 -> HardwareRegister.R12D;
                    case R13 -> HardwareRegister.R13D;
                    case R14 -> HardwareRegister.R14D;
                    case R15 -> HardwareRegister.R15D;
                    default -> throw new IllegalArgumentException();
                };
            } else if (width == Width.BIT8){
                return switch (this) {
                    case A -> HardwareRegister.AL;
                    case B -> HardwareRegister.BL;
                    case C -> HardwareRegister.CL;
                    case D -> HardwareRegister.DL;
                    case SI -> HardwareRegister.SIL;
                    case DI -> HardwareRegister.DIL;
                    case R8 -> HardwareRegister.R8B;
                    case R9 -> HardwareRegister.R9B;
                    case R10 -> HardwareRegister.R10B;
                    case R11 -> HardwareRegister.R11B;
                    case R12 -> HardwareRegister.R12B;
                    case R13 -> HardwareRegister.R13B;
                    case R14 -> HardwareRegister.R14B;
                    case R15 -> HardwareRegister.R15B;
                    default -> throw new IllegalArgumentException();
                };
            } else {
                throw new IllegalArgumentException();
            }
        }
    }

    public static final HardwareRegister RAX = new HardwareRegister(Group.A, "rax", Width.BIT64);
    public static final HardwareRegister RBX = new HardwareRegister(Group.B, "rbx", Width.BIT64);
    public static final HardwareRegister RCX = new HardwareRegister(Group.C, "rcx", Width.BIT64);
    public static final HardwareRegister RDX = new HardwareRegister(Group.D, "rdx", Width.BIT64);
    public static final HardwareRegister RSI = new HardwareRegister(Group.SI, "rsi", Width.BIT64);
    public static final HardwareRegister RDI = new HardwareRegister(Group.DI, "rdi", Width.BIT64);

    public static final HardwareRegister EAX = new HardwareRegister(Group.A, "eax", Width.BIT32);
    public static final HardwareRegister EBX = new HardwareRegister(Group.B, "ebx", Width.BIT32);
    public static final HardwareRegister ECX = new HardwareRegister(Group.C, "ecx", Width.BIT32);
    public static final HardwareRegister EDX = new HardwareRegister(Group.D, "edx", Width.BIT32);
    public static final HardwareRegister ESI = new HardwareRegister(Group.SI, "esi", Width.BIT32);
    public static final HardwareRegister EDI = new HardwareRegister(Group.DI, "edi", Width.BIT32);

    public static final HardwareRegister AL = new HardwareRegister(Group.A, "al", Width.BIT8);
    public static final HardwareRegister BL = new HardwareRegister(Group.B, "bl", Width.BIT8);
    public static final HardwareRegister CL = new HardwareRegister(Group.C, "cl", Width.BIT8);
    public static final HardwareRegister DL = new HardwareRegister(Group.D, "dl", Width.BIT8);
    public static final HardwareRegister SIL = new HardwareRegister(Group.SI, "sil", Width.BIT8);
    public static final HardwareRegister DIL = new HardwareRegister(Group.DI, "dil", Width.BIT8);

    public static final HardwareRegister R8 = new HardwareRegister(Group.R8, "r8", Width.BIT64);
    public static final HardwareRegister R9 = new HardwareRegister(Group.R9, "r9", Width.BIT64);
    public static final HardwareRegister R10 = new HardwareRegister(Group.R10,"r10", Width.BIT64);
    public static final HardwareRegister R11 = new HardwareRegister(Group.R11,"r11", Width.BIT64);
    public static final HardwareRegister R12 = new HardwareRegister(Group.R12,"r12", Width.BIT64);
    public static final HardwareRegister R13 = new HardwareRegister(Group.R13,"r13", Width.BIT64);
    public static final HardwareRegister R14 = new HardwareRegister(Group.R14,"r14", Width.BIT64);
    public static final HardwareRegister R15 = new HardwareRegister(Group.R15,"r15", Width.BIT64);

    public static final HardwareRegister R8D = new HardwareRegister(Group.R8, "r8d", Width.BIT32);
    public static final HardwareRegister R9D = new HardwareRegister(Group.R9, "r9d", Width.BIT32);
    public static final HardwareRegister R10D = new HardwareRegister(Group.R10, "r10d", Width.BIT32);
    public static final HardwareRegister R11D = new HardwareRegister(Group.R11, "r11d", Width.BIT32);
    public static final HardwareRegister R12D = new HardwareRegister(Group.R12, "r12d", Width.BIT32);
    public static final HardwareRegister R13D = new HardwareRegister(Group.R13, "r13d", Width.BIT32);
    public static final HardwareRegister R14D = new HardwareRegister(Group.R14, "r14d", Width.BIT32);
    public static final HardwareRegister R15D = new HardwareRegister(Group.R15, "r15d", Width.BIT32);

    public static final HardwareRegister R8B = new HardwareRegister(Group.R8, "r8b", Width.BIT8);
    public static final HardwareRegister R9B = new HardwareRegister(Group.R9, "r9b", Width.BIT8);
    public static final HardwareRegister R10B = new HardwareRegister(Group.R10, "r10b", Width.BIT8);
    public static final HardwareRegister R11B = new HardwareRegister(Group.R11, "r11b", Width.BIT8);
    public static final HardwareRegister R12B = new HardwareRegister(Group.R12, "r12b", Width.BIT8);
    public static final HardwareRegister R13B = new HardwareRegister(Group.R13, "r13b", Width.BIT8);
    public static final HardwareRegister R14B = new HardwareRegister(Group.R14, "r14b", Width.BIT8);
    public static final HardwareRegister R15B = new HardwareRegister(Group.R15, "r15b", Width.BIT8);

    // These are not general purpose register and are only ever used for stack operations.
    // Since we only target x86_64 we only need them in their 64bit variant.
    public static final HardwareRegister RSP = new HardwareRegister(Group.RSP, "rsp", Width.BIT64);
    public static final HardwareRegister RBP = new HardwareRegister(Group.RBP, "rbp", Width.BIT64);

    private final String name;
    private final Width width;

    // A hardware register with each of its different widths forms a group. (eg. rax, eax and al)
    private final Group group;

    private HardwareRegister(Group group, String name, Width width) {
        this.name = name;
        this.width = width;
        this.group = group;
    }

    public HardwareRegister forWidth(Register.Width width) {
        if (this.width == width) {
            return this;
        }

        return this.group.getRegister(width);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Width getWidth() {
        return this.width;
    }

    public Group getGroup() {
        return this.group;
    }
}
