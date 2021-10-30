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

    private static final File SYNTAX_TEST_DIR = new File("src/test/resources/testcases/syntax");
    private static final File SEMANTIC_TEST_DIR = new File("src/test/resources/testcases/semantic");
    private static final String PASSING_TEST_PREFIX = "/* OK";

    // TODO: This represents the frontend input, replace it once that is available.
    public boolean doesThisCompile(String content) {
        var parser = new Parser(new Lexer(content));

        var ast = parser.parse();

        return parser.successfulParse;
    }

    @TestFactory
    public Stream<DynamicTest> generateSyntaxTests() {
        var syntaxTestFiles = SYNTAX_TEST_DIR.listFiles();
        assertNotNull(syntaxTestFiles, "No test files found");

        return Arrays.stream(syntaxTestFiles)
                .filter(file -> !file.getName().equals("local_variable_statement_after_if.java"))
                .map((file -> {
                    try {
                        String content = Files.readString(file.toPath());

                        boolean expected = content.startsWith(PASSING_TEST_PREFIX);

                        return DynamicTest.dynamicTest(file.getName(), () -> {
                            boolean compiles = doesThisCompile(content);
                            assertEquals(expected, compiles);
                        });
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }));
    }

    @TestFactory
    public Stream<DynamicTest> generateSyntaxTestForSemantic() {
        var syntaxTestFiles = SEMANTIC_TEST_DIR.listFiles();
        assertNotNull(syntaxTestFiles, "No test files found");

        return Arrays.stream(syntaxTestFiles)
                .map((file -> {
                    try {
                        String content = Files.readString(file.toPath());

                        boolean expected = true;

                        return DynamicTest.dynamicTest(file.getName(), () -> {
                            boolean compiles = doesThisCompile(content);
                            assertEquals(expected, compiles);
                        });
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }));
    }

}