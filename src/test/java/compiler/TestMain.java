package compiler;

import org.junit.jupiter.api.Test;

public class TestMain {

    @Test
    public void testJavaSeventeenPreview() {
        // Demonstrate Java 17 with Preview features works
        Object a = 114;
        String formatted = switch (a) {
            case Integer i && i > 10 -> String.format("a large Integer %d", i);
            case Integer i -> String.format("a small Integer %d", i);
            default -> "something else";
        };
    }

}
