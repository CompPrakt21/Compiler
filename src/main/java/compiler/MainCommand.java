package compiler;

import compiler.ast.Program;
import compiler.diagnostics.CompilerMessageReporter;
import compiler.semantic.ConstantFolding;
import compiler.semantic.WellFormed;
import compiler.semantic.resolution.NameResolution;
import compiler.syntax.Lexer;
import compiler.syntax.Parser;
import compiler.syntax.Token;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;

@Command(name = "compiler", mixinStandardHelpOptions = true, version = "compiler 0.1.0",
        description = "MiniJava to x86 compiler")
public class MainCommand implements Callable<Integer> {

    @Spec
    CommandSpec spec;

    private static int callWithFileContent(File file, Function<String, Boolean> f) {
        try {
            String content = Files.readString(file.toPath());
            boolean error = f.apply(content);
            return error ? 1 : 0;
        } catch (FileNotFoundException | NoSuchFileException e) {
            System.err.format("error: Can not find file: '%s'\n", file.getName());
            return 1;
        } catch (IOException e) {
            System.err.format("error: Can not read file: '%s'\n", file.getName());
            return 1;
        }
    }

    @SuppressWarnings("unused")
    @Command(name = "--echo", description = "Echos back the input file.")
    public Integer callEcho(@Parameters(paramLabel = "FILE", description = "The file to echo.") File file) {
        return callWithFileContent(file, content -> {
            System.out.print(content);
            return false;
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--lextest", description = "Outputs lexed tokens for the input file")
    public Integer callLextest(@Parameters(paramLabel = "FILE", description = "The file to lxe.") File file) {
        return callWithFileContent(file, content -> {
            Lexer l = new Lexer(content);
            boolean error = false;
            loop:
            while (true) {
                Token t = l.nextToken();
                switch (t.type) {
                    case EOF -> {
                        System.out.println("EOF");
                        break loop;
                    }
                    case Error -> {
                        System.err.println("error: " + t.getErrorContent());
                        error = true;
                    }
                    case Identifier -> System.out.println("identifier " + t.getIdentContent());
                    case IntLiteral -> System.out.println("integer literal " + t.getIntLiteralContent());
                    default -> System.out.println(t.type.repr);
                }
            }
            return error;
        });
    }

    @FunctionalInterface
    private interface PostParseOperation {
        boolean run(CompilerMessageReporter r, Parser p, Program prog);
    }

    private static Integer callWithParsed(File file, PostParseOperation op) {
        return callWithFileContent(file, content -> {
            var reporter = new CompilerMessageReporter(new PrintWriter(System.err), content);
            var parser = new Parser(new Lexer(content), reporter);
            var ast = parser.parse();
            if (!parser.successfulParse) {
                reporter.finish();
                return true;
            }
            boolean error = op.run(reporter, parser, ast);
            reporter.finish();
            return error;
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--parsetest", description = "Checks whether the input file parses.")
    public Integer callParseTest(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> false);
    }

    @SuppressWarnings("unused")
    @Command(name = "--dump-dot-ast", description = "Generates a dot file with the ast.")
    public Integer callDumpAst(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            var resolution = NameResolution.performNameResolution(ast, reporter);

            var constantFolding = ConstantFolding.performConstantFolding(ast, Optional.of(reporter));

            var wellFormed = WellFormed.checkWellFormdness(ast, resolution, Optional.of(reporter));

            new DumpAst(new PrintWriter(System.out), resolution.definitions())
                    .addNodeAttribute("ty", resolution.expressionTypes())
                    .addNodeAttribute("const", constantFolding.constants())
                    .addNodeAttribute("local_vars", wellFormed.variableCounts())
                    .dump(ast);

            return !(resolution.successful() && wellFormed.correct() && constantFolding.successful());
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--print-ast", description = "Prett-prints the parsed AST.")
    public Integer printAst(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            System.out.println(AstPrinter.program(ast));
            return false;
        });
    }

    @FunctionalInterface
    private interface PostCheckOperation {
        boolean run(CompilerMessageReporter r, Program program, NameResolution.NameResolutionResult resolution, ConstantFolding.ConstantFoldingResult constants, WellFormed.WellFormedResult wellformed);
    }

    private static Integer callWithChecked(File file, PostCheckOperation op) {
        return callWithParsed(file, (reporter, parser, ast) -> {
            var nameResolutionResult = NameResolution.performNameResolution(ast, reporter);

            var constantFolding = ConstantFolding.performConstantFolding(ast, Optional.of(reporter));

            var wellFormed = WellFormed.checkWellFormdness(ast, nameResolutionResult, Optional.of(reporter));

            var opResult = op.run(reporter, ast, nameResolutionResult, constantFolding, wellFormed);

            return opResult;
        });
    }

    @SuppressWarnings("unused")
    @Command(name = "--check", description = "Performs semantic analysis of the input.")
    public Integer check(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithChecked(file, (reporter, ast, resolution, constants, wellformed) -> !(resolution.successful() && wellformed.correct() && constants.successful()));
    }

    @SuppressWarnings("unused")
    @Command(name = "--firm-version", description = "Print Firm version.")
    public Integer firmVersion() {
        FirmTest.printVersion();
        return 0;
    }

    @SuppressWarnings("unused")
    @Command(name = "--translate", description = "Translate to libFirm and dump.")
    public Integer translate(@Parameters(paramLabel = "FILE", description = "The file to parse.") File file) {
        return callWithChecked(file, (reporter, ast, resolution, constants, wellFormed) -> {
            var translation = new Translation(resolution, constants, wellFormed);
            translation.translate(ast);
            return false;
        });
    }

    @Override
    public Integer call() {
        // For now, a subcommand is required.
        // Once compiling is implemented, that will be the default instead,
        throw new ParameterException(spec.commandLine(), "Specify a subcommand");
    }

    public static void main(String[] args) {
        // Debug picocli by uncommenting this:
        // System.setProperty("picocli.trace", "DEBUG");
        int exitCode = new CommandLine(new MainCommand()).execute(args);
        System.exit(exitCode);
    }
}
