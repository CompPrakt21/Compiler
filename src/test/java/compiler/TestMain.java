package compiler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestMain {

    @Test
    public void testRandomInt() {
        assertEquals(12, MainCommand.getRandomInt());
    }

}
