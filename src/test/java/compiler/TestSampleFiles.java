package compiler;

import compiler.diagnostics.CompilerMessageReporter;
import compiler.resolution.NameResolution;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class TestSampleFiles {

    private static final File SYNTAX_TEST_DIR = new File("src/test/resources/testcases/syntax");
    private static final File SEMANTIC_TEST_DIR = new File("src/test/resources/testcases/semantic");
    private static final String PASSING_TEST_PREFIX = "/* OK";

    public boolean doesThisParse(String content) {
        var reporter = new CompilerMessageReporter(new PrintWriter(System.err), content);
        var parser = new Parser(new Lexer(content), reporter);

        var ast = parser.parse();

        return parser.successfulParse;
    }

    @TestFactory
    public Stream<DynamicTest> generateSyntaxTests() {
        var syntaxTestFiles = SYNTAX_TEST_DIR.listFiles();
        assertNotNull(syntaxTestFiles, "No test files found");

        return Arrays.stream(syntaxTestFiles)
                .map((file -> {
                    try {
                        String content = Files.readString(file.toPath());

                        boolean expected = content.startsWith(PASSING_TEST_PREFIX);

                        return DynamicTest.dynamicTest(file.getName(), () -> {
                            boolean compiles = doesThisParse(content);
                            assertEquals(expected, compiles);
                        });
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }));
    }

    @TestFactory
    public Stream<DynamicTest> generateIdempotenceTests() {
        var syntaxTestFiles = SYNTAX_TEST_DIR.listFiles();
        var semanticTestFiles = SEMANTIC_TEST_DIR.listFiles();
        assertNotNull(syntaxTestFiles, "No test files found");
        assertNotNull(semanticTestFiles, "No test files found");

        record TestCase(String name, String content) {
        }
        var syntaxTestContents = Arrays.stream(syntaxTestFiles)
                .map(file -> {
                    try {
                        return new TestCase(file.getName(), Files.readString(file.toPath()));
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }).filter(testCase -> testCase.content.startsWith(PASSING_TEST_PREFIX));
        var semanticTestContents = Arrays.stream(semanticTestFiles)
                .map(file -> {
                    try {
                        return new TestCase(file.getName(), Files.readString(file.toPath()));
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                });
        var testContents = Stream.concat(syntaxTestContents, semanticTestContents).collect(Collectors.toList());

        return testContents.stream()
                .map(testCase ->
                        DynamicTest.dynamicTest(testCase.name, () -> {
                            var firstParser = new Parser(new Lexer(testCase.content));
                            var firstAst = firstParser.parse();
                            assertTrue(firstParser.successfulParse);
                            String firstOutput = AstPrinter.print(firstAst);
                            var secondParser = new Parser(new Lexer(firstOutput));
                            var secondAst = secondParser.parse();
                            assertTrue(secondParser.successfulParse);
                            String secondOutput = AstPrinter.print(secondAst);
                            assertEquals(firstOutput, secondOutput);
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
                            boolean compiles = doesThisParse(content);
                            assertEquals(expected, compiles);
                        });
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }));
    }

    public boolean doesThisCheck(String content) {
        var reporter = new CompilerMessageReporter(new PrintWriter(System.err), content);
        var parser = new Parser(new Lexer(content), reporter);

        var ast = parser.parse();

        var names = NameResolution.performNameResolution(ast, reporter);

        try {
            DumpAst.dump(new PrintWriter(new BufferedOutputStream(new FileOutputStream("astDump.dot"))), ast, names.definitions());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return parser.successfulParse;
    }

    /*@TestFactory
    public Stream<DynamicTest> generateSemanticTestForSemantic() {
        var syntaxTestFiles = SEMANTIC_TEST_DIR.listFiles();
        assertNotNull(syntaxTestFiles, "No test files found");

        return Arrays.stream(syntaxTestFiles)
                .map((file -> {
                    try {
                        String content = Files.readString(file.toPath());

                        boolean expected = true;

                        return DynamicTest.dynamicTest(file.getName(), () -> {
                            boolean compiles = doesThisCheck(content);
                            //assertEquals(expected, compiles);
                        });
                    } catch (IOException e) {
                        fail(e);
                        return null;
                    }
                }));
    }*/
}