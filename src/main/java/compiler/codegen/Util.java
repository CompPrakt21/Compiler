package compiler.codegen;

public class Util {
    public static boolean fitsInto32Bit(Long value) {
        return (long)Integer.MIN_VALUE <= value && value <= (long)Integer.MAX_VALUE;
    }
}
