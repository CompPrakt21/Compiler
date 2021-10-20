package compiler;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class TestSampleFiles {

    private static final File TEST_DIR = new File("src/test/resources/testcases");
    private static final String PASSING_TEST_PREFIX = "// OK";

    // TODO: This represents the frontend input, replace it once that is available.
    public boolean doesThisCompile(String content) {
        //System.out.println(content);
        return true;
    }

    @TestFactory
    public Stream<DynamicTest> generateTests() {
        var testFiles = TEST_DIR.listFiles();
        assertNotNull(testFiles, "No test files found");

        return Arrays.stream(testFiles).map((file -> {
            try {
                String content = Files.readString(file.toPath());
                boolean expected = content.startsWith(PASSING_TEST_PREFIX);

                return DynamicTest.dynamicTest(file.getName(), () -> {
                    boolean compiles = doesThisCompile(content);
                    // TODO: Enable this once testing should start
                    //assertEquals(compiles, expected);
                });
            } catch (IOException e) {
                fail(e);
                return null;
            }
        }));
    }

}